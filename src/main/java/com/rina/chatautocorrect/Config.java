package com.rina.chatautocorrect;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Saves the on/off setting to config/chat-autocorrect.properties. */
public final class Config {
    private Config() {}

    public static boolean enabled = true;
    public static boolean spellCheck = true;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("chat-autocorrect.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) { save(); return; }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            spellCheck = Boolean.parseBoolean(p.getProperty("spellCheck", "true"));
        } catch (IOException ignored) {}
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("spellCheck", Boolean.toString(spellCheck));
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Chat Autocorrect");
        } catch (IOException ignored) {}
    }
}
