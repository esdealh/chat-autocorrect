package com.rina.chatautocorrect;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Settings live in the .minecraft/config folder:
 *   chat-autocorrect.properties     on/off switches
 *   chat-autocorrect/fixes.txt      your own fixes, "wrong = right" per line
 *   chat-autocorrect/never-change.txt  words to leave alone
 * The text files can also be edited by hand.
 */
public final class Config {
    private Config() {}

    public static boolean enabled = true;
    public static boolean spellCheck = true;
    public static Map<String, String> fixes = new LinkedHashMap<>();
    public static Set<String> neverChange = new LinkedHashSet<>();

    private static Path dir() { return FabricLoader.getInstance().getConfigDir(); }
    private static Path settingsFile() { return dir().resolve("chat-autocorrect.properties"); }
    private static Path fixesFile() { return dir().resolve("chat-autocorrect").resolve("fixes.txt"); }
    private static Path neverChangeFile() { return dir().resolve("chat-autocorrect").resolve("never-change.txt"); }

    public static void load() {
        Path f = settingsFile();
        if (Files.exists(f)) {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f)) {
                p.load(r);
                enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
                spellCheck = Boolean.parseBoolean(p.getProperty("spellCheck", "true"));
            } catch (IOException ignored) {}
        }
        fixes = WordLists.parseFixes(read(fixesFile()));
        neverChange = WordLists.parseWords(read(neverChangeFile()));
        apply();
        save();
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("spellCheck", Boolean.toString(spellCheck));
        try (Writer w = Files.newBufferedWriter(settingsFile())) {
            p.store(w, "Chat Autocorrect");
        } catch (IOException ignored) {}
        write(fixesFile(), WordLists.fixesToText(fixes));
        write(neverChangeFile(), WordLists.wordsToText(neverChange));
    }

    /** Push the current settings into the autocorrect logic. */
    public static void apply() {
        AutoCorrect.spellCheck = spellCheck;
        AutoCorrect.customFixes = Map.copyOf(fixes);
        AutoCorrect.neverChange = Set.copyOf(neverChange);
    }

    private static String read(Path f) {
        try {
            return Files.exists(f) ? Files.readString(f, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static void write(Path f, String text) {
        try {
            Files.createDirectories(f.getParent());
            Files.writeString(f, text, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
