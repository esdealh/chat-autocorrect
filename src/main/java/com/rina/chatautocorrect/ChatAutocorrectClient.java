package com.rina.chatautocorrect;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ChatAutocorrectClient implements ClientModInitializer {
    /** Start a message with this to send it exactly as typed. */
    private static final String RAW_PREFIX = "\\";

    @Override
    public void onInitializeClient() {
        Config.load();

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
                        .then(literal("toggle").executes(ctx -> set(ctx.getSource(), !Config.enabled)))));
    }

    private static int set(FabricClientCommandSource source, boolean value) {
        Config.enabled = value;
        Config.save();
        return status(source);
    }

    private static int status(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("Chat Autocorrect is ")
                .append(Config.enabled
                        ? Text.literal("ON").formatted(Formatting.GREEN)
                        : Text.literal("OFF").formatted(Formatting.RED))
                .append(Text.literal(". Start a message with \\ to skip it once.").formatted(Formatting.GRAY)));
        return Command.SINGLE_SUCCESS;
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
