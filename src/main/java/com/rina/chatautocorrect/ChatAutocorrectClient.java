package com.rina.chatautocorrect;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ChatAutocorrectClient implements ClientModInitializer {
    /** Start a message with this to send it exactly as typed. */
    private static final String RAW_PREFIX = "\\";

    @Override
    public void onInitializeClient() {
        Config.load();
        loadDictionary();

        ClientSendMessageEvents.MODIFY_CHAT.register(message -> {
            if (message.startsWith(RAW_PREFIX) && message.length() > 1) return message.substring(1);
            if (!Config.enabled) return message;
            try {
                return AutoCorrect.correct(message, onlinePlayerNames());
            } catch (Exception e) {
                return message; // never block a message because of a bug here
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("autocorrect")
                        .executes(ctx -> status(ctx.getSource()))
                        .then(literal("on").executes(ctx -> set(ctx.getSource(), true)))
                        .then(literal("off").executes(ctx -> set(ctx.getSource(), false)))
                        .then(literal("toggle").executes(ctx -> set(ctx.getSource(), !Config.enabled)))
                        .then(literal("spelling")
                                .then(literal("on").executes(ctx -> setSpelling(ctx.getSource(), true)))
                                .then(literal("off").executes(ctx -> setSpelling(ctx.getSource(), false))))
                        // /autocorrect add gn good night
                        .then(literal("add").then(argument("wrong", StringArgumentType.word())
                                .then(argument("right", StringArgumentType.greedyString()).executes(ctx -> {
                                    String wrong = StringArgumentType.getString(ctx, "wrong").toLowerCase(Locale.ROOT);
                                    String right = StringArgumentType.getString(ctx, "right").strip();
                                    Config.fixes.put(wrong, right);
                                    return saved(ctx.getSource(), "Will now change \"" + wrong + "\" to \"" + right + "\".");
                                }))))
                        // /autocorrect remove gn
                        .then(literal("remove").then(argument("word", StringArgumentType.word()).executes(ctx -> {
                            String w = StringArgumentType.getString(ctx, "word").toLowerCase(Locale.ROOT);
                            boolean removed = Config.fixes.remove(w) != null | Config.neverChange.remove(w);
                            return saved(ctx.getSource(), removed ? "Removed \"" + w + "\"." : "\"" + w + "\" wasn't in your lists.");
                        })))
                        // /autocorrect ignore esdealh
                        .then(literal("ignore").then(argument("word", StringArgumentType.word()).executes(ctx -> {
                            String w = StringArgumentType.getString(ctx, "word").toLowerCase(Locale.ROOT);
                            Config.neverChange.add(w);
                            return saved(ctx.getSource(), "Will never change \"" + w + "\".");
                        })))));
    }

    private static int saved(FabricClientCommandSource source, String message) {
        Config.apply();
        Config.save();
        source.sendFeedback(Text.literal(message).formatted(Formatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static void loadDictionary() {
        try (InputStream words = ChatAutocorrectClient.class.getResourceAsStream("/assets/chat-autocorrect/words.txt");
             InputStream extra = ChatAutocorrectClient.class.getResourceAsStream("/assets/chat-autocorrect/extra-words.txt")) {
            if (words != null) SpellCheck.load(words, extra);
        } catch (Exception e) {
            // spell check just stays off; the rest of the mod still works
        }
    }

    private static int setSpelling(FabricClientCommandSource source, boolean value) {
        Config.spellCheck = value;
        Config.apply();
        Config.save();
        return status(source);
    }

    private static int set(FabricClientCommandSource source, boolean value) {
        Config.enabled = value;
        Config.save();
        return status(source);
    }

    private static int status(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("Chat Autocorrect is ")
                .append(onOff(Config.enabled))
                .append(Text.literal(", spell check is "))
                .append(onOff(Config.spellCheck && SpellCheck.isLoaded()))
                .append(Text.literal(". Start a message with \\ to skip it once. Add words with /autocorrect add or ignore.").formatted(Formatting.GRAY)));
        return Command.SINGLE_SUCCESS;
    }

    private static Text onOff(boolean on) {
        return on ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED);
    }

    private static Set<String> onlinePlayerNames() {
        Set<String> names = new HashSet<>();
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return names;
        for (PlayerListEntry entry : handler.getPlayerList()) {
            names.add(entry.getProfile().getName().toLowerCase(Locale.ROOT));
        }
        return names;
    }
}
