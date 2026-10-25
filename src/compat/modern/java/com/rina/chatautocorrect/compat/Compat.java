package com.rina.chatautocorrect.compat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

/** Version-specific bits for Minecraft 1.21.6 and newer (text boxes are made with a builder). */
public final class Compat {
    private Compat() {}

    public static EditBoxWidget editBox(TextRenderer font, int x, int y, int width, int height, Text placeholder, Text message) {
        return EditBoxWidget.builder().x(x).y(y).placeholder(placeholder).build(font, width, height, message);
    }

    /** Player name from a GameProfile. The method was renamed in 1.21.9 (getName -> name), so look it up by name. */
    public static String profileName(Object profile) {
        if (profile == null) return null;
        for (String method : new String[]{"name", "getName"}) {
            try {
                Object v = profile.getClass().getMethod(method).invoke(profile);
                if (v instanceof String s) return s;
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }
}
