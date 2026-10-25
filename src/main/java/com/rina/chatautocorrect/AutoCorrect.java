package com.rina.chatautocorrect;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text logic, no Minecraft classes, so it can be tested on its own.
 */
public final class AutoCorrect {
    private AutoCorrect() {}

    /** The player's own fixes (lowercase typo -> fix). Checked before everything else. */
    public static volatile Map<String, String> customFixes = Map.of();
    /** Words the player never wants changed (lowercase). Treated like player names. */
    public static volatile Set<String> neverChange = Set.of();

    /** Use the dictionary spell checker for words not in the fix list. */
    public static boolean spellCheck = true;

    /** Vanilla chat limit. If a fix would go over it, the original message is sent. */
    public static final int MAX_LENGTH = 256;

    /** lowercase typo -> correct form */
    private static final Map<String, String> WORDS = new HashMap<>();

    /** Words that end a message without needing a period. */
    private static final Set<String> NO_PERIOD_WORDS = Set.of(
            "lol", "lmao", "lmfao", "rofl", "xd", "gg", "ggs", "gl", "glhf", "brb", "afk",
            "ty", "thx", "np", "omg", "ok", "k", "kk", "yw", "idk", "imo", "tbh", "btw", "rn", "pls", "plz", "ez", "fr", "ngl", "bruh", "xdd", "wp", "ggwp");

    private static final Set<String> QUESTION_STARTERS = Set.of(
            "who", "what", "when", "where", "why", "how", "which", "whose", "whom",
            "is", "are", "am", "was", "were", "does", "did", "can", "could", "would", "should",
            "isn't", "aren't", "doesn't", "didn't", "can't", "couldn't", "wouldn't", "shouldn't",
            "won't", "anyone", "anybody", "somebody", "someone", "wanna", "what's", "where's", "who's", "how's");

    /** These only start a question when followed by a pronoun ("do you" yes, "do it" no). */
    private static final Set<String> WEAK_QUESTION_STARTERS = Set.of("do", "will", "have", "has", "shall", "may");
    private static final Set<String> PRONOUNS = Set.of(
            "i", "you", "u", "we", "they", "he", "she", "it", "anyone", "someone", "somebody", "anybody", "there");

    static {
        String[][] pairs = {
                {"i", "I"}, {"im", "I'm"}, {"i'm", "I'm"}, {"ive", "I've"}, {"i've", "I've"},
                {"i'll", "I'll"}, {"i'd", "I'd"},
                {"dont", "don't"}, {"cant", "can't"}, {"wont", "won't"}, {"didnt", "didn't"},
                {"doesnt", "doesn't"}, {"isnt", "isn't"}, {"arent", "aren't"}, {"wasnt", "wasn't"},
                {"werent", "weren't"}, {"havent", "haven't"}, {"hasnt", "hasn't"}, {"hadnt", "hadn't"},
                {"couldnt", "couldn't"}, {"shouldnt", "shouldn't"}, {"wouldnt", "wouldn't"},
                {"aint", "ain't"}, {"thats", "that's"}, {"whats", "what's"}, {"theres", "there's"},
                {"youre", "you're"}, {"theyre", "they're"}, {"youve", "you've"}, {"theyve", "they've"},
                {"weve", "we've"}, {"youll", "you'll"}, {"theyll", "they'll"}, {"itll", "it'll"},
                {"teh", "the"}, {"adn", "and"}, {"recieve", "receive"}, {"becuase", "because"},
                {"beacuse", "because"}, {"definately", "definitely"}, {"definitly", "definitely"},
                {"alot", "a lot"}, {"wierd", "weird"}, {"untill", "until"}, {"seperate", "separate"},
                {"thier", "their"}, {"freind", "friend"}, {"tommorow", "tomorrow"}, {"tomorow", "tomorrow"},
                {"wich", "which"}, {"occured", "occurred"}, {"truely", "truly"}, {"basicly", "basically"},
                {"realy", "really"}, {"finaly", "finally"}, {"knwo", "know"}, {"waht", "what"},
                {"taht", "that"}, {"jsut", "just"}, {"hte", "the"}, {"thnaks", "thanks"},
                // common chat misspellings the spell checker would guess wrong
                {"helo", "hello"}, {"wher", "where"}, {"giv", "give"}, {"hav", "have"}, {"wanst", "wasn't"}, {"probly", "probably"}, {"probaly", "probably"}, {"thas", "that's"},
                {"becuz", "because"}, {"becus", "because"}, {"bcuz", "because"}, {"unfur", "unfair"},
                {"dis", "this"}, {"wat", "what"}, {"wut", "what"}, {"pls", "pls"}, {"ofc", "ofc"},
                {"didnt", "didn't"}, {"idk", "idk"}, {"ya'll", "y'all"}, {"yall", "y'all"},
                {"lets", "let's"}, {"hes", "he's"}, {"shes", "she's"}, {"wouldve", "would've"},
                {"couldve", "could've"}, {"shouldve", "should've"}, {"wheres", "where's"}, {"whos", "who's"},
                {"hows", "how's"}, {"heres", "here's"}, {"everytime", "every time"}, {"noone", "no one"},
        };
        for (String[] p : pairs) WORDS.put(p[0], p[1]);
    }

    // leading symbols | word (letters, digits, apostrophes) | trailing symbols
    private static final Pattern TOKEN = Pattern.compile("^([^\\p{L}\\p{N}]*)([\\p{L}\\p{N}'’]*?)([^\\p{L}\\p{N}]*)$");
    private static final Pattern EMOTE = Pattern.compile("^(?:[:;=8xX][-o^']?[)(\\]\\[DPpOo3/\\\\|*]+|<3+|\\^\\^|\\^_\\^|[oO]_[oO]|-_-|T_T|:[a-z_]+:)$");

    /**
     * @param message     the chat message as typed
     * @param playerNames lowercase names of online players (left exactly as typed)
     */
    public static String correct(String message, Set<String> playerNames) {
        if (message == null) return null;
        String s = message.strip().replaceAll("\\s{2,}", " ");
        if (s.isEmpty()) return message;

        s = s.replaceAll(" +,", ",");                 // "hi , there" -> "hi, there"
        s = s.replaceAll(",(?=\\p{L})", ", ");         // "hi,there"   -> "hi, there"

        String[] tokens = s.split(" ");
        boolean capNext = true;

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (isUrl(t)) { capNext = false; continue; }
            if (isEmote(t)) continue;

            Matcher m = TOKEN.matcher(t);
            if (!m.matches()) {
                // Something like "co-op" or "a.m." - only capitalize it.
                if (capNext) tokens[i] = capitalize(t);
                capNext = endsSentence(t);
                continue;
            }

            String prefix = m.group(1), core = m.group(2), suffix = m.group(3);
            String lower = core.toLowerCase(Locale.ROOT);
            boolean isName = playerNames.contains(lower) || neverChange.contains(lower);

            if (!core.isEmpty() && !isName) {
                String fix = customFixes.get(lower);
                if (fix == null) fix = WORDS.get(lower);
                // Capitalized mid-sentence = probably a name ("ask Bob"), so don't spell check it
                boolean looksLikeName = !capNext && Character.isUpperCase(core.charAt(0)) && !core.equals(core.toUpperCase(Locale.ROOT));
                if (fix == null && spellCheck && !looksLikeName) fix = SpellCheck.suggest(lower);
                if (fix != null) core = matchCase(core, fix);
                if (capNext && !NO_PERIOD_WORDS.contains(core.toLowerCase(Locale.ROOT))) core = capitalize(core);
            }

            tokens[i] = prefix + core + suffix;
            if (!core.isEmpty() || !suffix.isEmpty()) capNext = endsSentence(suffix);
        }

        String out = String.join(" ", tokens);

        // Ending punctuation (only for messages of 2+ words)
        String last = tokens[tokens.length - 1];
        String lastLower = last.toLowerCase(Locale.ROOT);
        char end = out.charAt(out.length() - 1);
        if (tokens.length >= 2
                && (Character.isLetterOrDigit(end) || end == '\'' || end == '’')
                && !isUrl(last) && !isEmote(last)
                && !NO_PERIOD_WORDS.contains(lastLower)
                && !playerNames.contains(lastLower) && !neverChange.contains(lastLower)) {
            out += isQuestion(lastSentence(tokens)) ? "?" : ".";
        }

        return out.length() > MAX_LENGTH ? message : out;
    }

    /** A sentence is a question if it, or a clause after a comma ("hey, how are you"), starts like one. */
    private static boolean isQuestion(String[] sentence) {
        for (int i = 0; i < sentence.length; i++) {
            if (i > 0 && !sentence[i - 1].endsWith(",")) continue;
            String first = word(sentence[i]);
            if (QUESTION_STARTERS.contains(first)) return true;
            if (WEAK_QUESTION_STARTERS.contains(first) && i + 1 < sentence.length) {
                String next = word(sentence[i + 1]);
                // "do it now" is an instruction, "will it work" is a question
                if (PRONOUNS.contains(next) && !(first.equals("do") && next.equals("it"))) return true;
            }
        }
        return false;
    }

    /** Tokens after the last sentence-ending punctuation. */
    private static String[] lastSentence(String[] tokens) {
        int start = 0;
        for (int i = 0; i < tokens.length - 1; i++) {
            if (!isUrl(tokens[i]) && endsSentence(tokens[i])) start = i + 1;
        }
        String[] r = new String[tokens.length - start];
        System.arraycopy(tokens, start, r, 0, r.length);
        return r;
    }

    private static String word(String token) {
        return token.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}'’]+$", "")
                .replace('’', '\'').toLowerCase(Locale.ROOT);
    }

    private static boolean endsSentence(String s) {
        String trimmed = s.replaceAll("[\"')\\]]+$", "");
        return !trimmed.isEmpty() && ".!?".indexOf(trimmed.charAt(trimmed.length() - 1)) >= 0;
    }

    private static boolean isUrl(String t) {
        String l = t.toLowerCase(Locale.ROOT);
        return l.contains("://") || l.startsWith("www.") || l.matches(".*\\.(com|net|org|gg|io|me|tv|co|de|uk)(/.*)?");
    }

    private static boolean isEmote(String t) {
        return EMOTE.matcher(t).matches();
    }

    private static String capitalize(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                return Character.isLowerCase(c) ? s.substring(0, i) + Character.toUpperCase(c) + s.substring(i + 1) : s;
            }
            if (Character.isDigit(c)) return s;
        }
        return s;
    }

    /** Keeps SHOUTING and Capitalized words that way after a fix. */
    private static String matchCase(String original, String fix) {
        if (original.length() > 1 && original.equals(original.toUpperCase(Locale.ROOT))) return fix.toUpperCase(Locale.ROOT);
        if (Character.isUpperCase(original.charAt(0))) return capitalize(fix);
        return fix;
    }
}
