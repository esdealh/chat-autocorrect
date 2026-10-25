package com.rina.chatautocorrect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Small spell checker: finds the most likely known word within 1-2 typing mistakes
 * (missing, extra, wrong or swapped letters), preferring common words.
 * Pure Java, no Minecraft classes.
 */
public final class SpellCheck {
    private SpellCheck() {}

    /** word -> rank (1 = most common) */
    private static final Map<String, Integer> RANK = new HashMap<>();
    /** Rank given to Minecraft/slang words so they win over obscure dictionary words. */
    private static final int EXTRA_WORD_RANK = 2000;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    // Scoring knobs (higher score wins)
    private static final double COST_WEIGHT = 5.0;           // how much one typing mistake outweighs word frequency
    private static final double FIRST_LETTER_PENALTY = 3.0;  // people rarely get the first letter wrong
    private static final int MAX_RANK_FOR_TWO_EDITS = 12000; // only "fix" 2 mistakes into fairly common words

    // Keyboard neighbours (QWERTY plus QWERTZ y/z), used to make "next key" slips cheap
    private static final String[] KEY_ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    private static final Set<String> ADJACENT = new HashSet<>();
    static {
        for (int r = 0; r < KEY_ROWS.length; r++) {
            String row = KEY_ROWS[r];
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (i + 1 < row.length()) addAdjacent(c, row.charAt(i + 1));
                if (r + 1 < KEY_ROWS.length) {
                    String below = KEY_ROWS[r + 1];
                    if (i < below.length()) addAdjacent(c, below.charAt(i));
                    if (i - 1 >= 0 && i - 1 < below.length()) addAdjacent(c, below.charAt(i - 1));
                }
            }
        }
        addAdjacent('z', 't'); addAdjacent('z', 'u'); addAdjacent('z', 'h'); // QWERTZ
        addAdjacent('y', 'a'); addAdjacent('y', 's'); addAdjacent('y', 'x');
    }
    private static void addAdjacent(char a, char b) { ADJACENT.add("" + a + b); ADJACENT.add("" + b + a); }

    private static boolean loaded;

    public static synchronized void load(InputStream wordsByFrequency, InputStream extraWords) throws IOException {
        RANK.clear();
        try (BufferedReader r = reader(wordsByFrequency)) {
            String line;
            int rank = 1;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) RANK.putIfAbsent(line, rank++);
            }
        }
        if (extraWords != null) {
            try (BufferedReader r = reader(extraWords)) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    for (String w : line.trim().split("\\s+")) {
                        if (w.isEmpty()) continue;
                        RANK.merge(w, EXTRA_WORD_RANK, Math::min);
                        RANK.merge(w + "s", EXTRA_WORD_RANK, Math::min); // plurals: enchants, elytras, totems
                    }
                }
            }
        }
        loaded = !RANK.isEmpty();
    }

    private static BufferedReader reader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public static boolean isLoaded() { return loaded; }

    public static boolean isKnown(String lowerWord) { return RANK.containsKey(lowerWord); }

    /** @return a correction for a lowercase word, or null to leave it alone */
    public static String suggest(String word) {
        if (!loaded || word.length() < 4 || RANK.containsKey(word)) return null;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c < 'a' || c > 'z') return null; // skip words with digits, apostrophes, accents
        }
        if (word.matches(".*(.)\\1\\1.*")) return null;        // "sooo", "hmmm" - stretched on purpose
        if (word.matches("[aeh]*h[aeh]+|[aeh]+h[aeh]*")) return null; // "hahaha", "hehe", "ahaha"
        // "heyy", "broo", "pleasee" - an extra last letter on a real word is on purpose
        int n = word.length();
        if (n <= 6 && word.charAt(n - 1) == word.charAt(n - 2) && RANK.containsKey(word.substring(0, n - 1))) return null;

        // Gather everything known within 2 edits, then rank by how cheap the mistakes are + how common the word is
        Set<String> candidates = new HashSet<>();
        Set<String> oneEdit = edits(word);
        for (String c : oneEdit) if (RANK.containsKey(c)) candidates.add(c);
        if (word.length() >= 5) {
            for (String e : oneEdit) {
                for (String c : edits(e)) {
                    Integer rank = RANK.get(c);
                    if (rank != null && rank <= MAX_RANK_FOR_TWO_EDITS) candidates.add(c);
                }
            }
        }

        // Short words are easy to mistake for other words, so allow less change
        double maxCost = word.length() <= 5 ? 1.0 : word.length() <= 7 ? 1.6 : 2.0;
        String best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (String c : candidates) {
            double cost = cost(word, c);
            boolean firstLetterChanged = c.charAt(0) != word.charAt(0);
            if (cost > maxCost || (firstLetterChanged && cost > 0.6)) continue;
            double s = -Math.log(RANK.get(c)) - COST_WEIGHT * cost - (firstLetterChanged ? FIRST_LETTER_PENALTY : 0);
            if (s > bestScore) { bestScore = s; best = c; }
        }
        return best;
    }

    /**
     * Weighted edit distance: a doubled/undoubled letter (helo/hello) or a swap is cheaper than
     * a random wrong letter, and hitting a neighbouring key is cheaper than a far one.
     */
    static double cost(String a, String b) {
        int n = a.length(), m = b.length();
        double[][] d = new double[n + 1][m + 1];
        for (int i = 1; i <= n; i++) d[i][0] = d[i - 1][0] + indelCost(a, i - 1);
        for (int j = 1; j <= m; j++) d[0][j] = d[0][j - 1] + indelCost(b, j - 1);
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                char x = a.charAt(i - 1), y = b.charAt(j - 1);
                double sub = x == y ? 0 : ADJACENT.contains("" + x + y) ? 0.6 : 1.0;
                double v = Math.min(d[i - 1][j - 1] + sub,
                           Math.min(d[i - 1][j] + indelCost(a, i - 1), d[i][j - 1] + indelCost(b, j - 1)));
                if (i > 1 && j > 1 && x == b.charAt(j - 2) && a.charAt(i - 2) == y) v = Math.min(v, d[i - 2][j - 2] + 0.6);
                d[i][j] = v;
            }
        }
        return d[n][m];
    }

    /** Adding/removing a letter that sits next to the same letter ("helo" vs "hello") is a common slip. */
    private static double indelCost(String s, int i) {
        char c = s.charAt(i);
        boolean doubled = (i > 0 && s.charAt(i - 1) == c) || (i + 1 < s.length() && s.charAt(i + 1) == c);
        return doubled ? 0.3 : 1.0;
    }

    /** Every string one deletion, swap, replacement or insertion away. */
    private static Set<String> edits(String w) {
        Set<String> out = new HashSet<>();
        int n = w.length();
        for (int i = 0; i <= n; i++) {
            String left = w.substring(0, i), right = w.substring(i);
            if (!right.isEmpty()) out.add(left + right.substring(1));                                   // delete
            if (right.length() > 1) out.add(left + right.charAt(1) + right.charAt(0) + right.substring(2)); // swap
            for (int k = 0; k < ALPHABET.length(); k++) {
                char ch = ALPHABET.charAt(k);
                if (!right.isEmpty()) out.add(left + ch + right.substring(1));                        // replace
                out.add(left + ch + right);                                                          // insert
            }
        }
        out.remove(w);
        return out;
    }
}
