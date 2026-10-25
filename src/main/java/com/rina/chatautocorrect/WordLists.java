package com.rina.chatautocorrect;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns the text in the settings boxes / config files into word lists and back.
 * Pure Java, no Minecraft classes.
 *
 * Fixes:        one per line, "wrong = right"  (also accepts "wrong -> right" or "wrong: right")
 * Never change: words separated by spaces, commas or new lines
 */
public final class WordLists {
    private WordLists() {}

    public static Map<String, String> parseFixes(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        if (text == null) return out;
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s*(?:=|->|:)\\s*", 2);
            if (parts.length != 2) continue;
            String wrong = parts[0].strip().toLowerCase(Locale.ROOT);
            String right = parts[1].strip();
            // The thing being fixed must be a single word; the fix can be several ("gn = good night")
            if (wrong.isEmpty() || right.isEmpty() || wrong.contains(" ")) continue;
            out.put(wrong, right);
        }
        return out;
    }

    public static String fixesToText(Map<String, String> fixes) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fixes.entrySet()) {
            sb.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    public static Set<String> parseWords(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) return out;
        for (String w : text.split("[\\s,;]+")) {
            w = w.strip().toLowerCase(Locale.ROOT);
            if (!w.isEmpty() && !w.startsWith("#")) out.add(w);
        }
        return out;
    }

    public static String wordsToText(Set<String> words) {
        return String.join("\n", words) + (words.isEmpty() ? "" : "\n");
    }
}
