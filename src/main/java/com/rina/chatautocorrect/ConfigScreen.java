package com.rina.chatautocorrect;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import com.rina.chatautocorrect.compat.Compat;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Settings screen, opened from Mod Menu's "Configure" button. */
public class ConfigScreen extends Screen {
    private final Screen parent;

    private boolean enabled = Config.enabled;
    private boolean spellCheck = Config.spellCheck;

    private EditBoxWidget fixesBox;
    private EditBoxWidget neverChangeBox;
    // Keep what was typed if the window is resized (init() runs again and rebuilds the boxes)
    private String fixesText = WordLists.fixesToText(Config.fixes);
    private String neverChangeText = WordLists.wordsToText(Config.neverChange);

    private static final int TOP = 32;
    private static final int LABEL_Y = TOP + 28;
    private static final int BOX_Y = LABEL_Y + 12;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Chat Autocorrect"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int gap = 10;
        int colWidth = Math.min(220, (this.width - 3 * gap) / 2);
        int left = this.width / 2 - colWidth - gap / 2;
        int right = this.width / 2 + gap / 2;
        int boxHeight = Math.max(40, this.height - BOX_Y - 40);

        // On/off switches
        addDrawableChild(ButtonWidget.builder(toggleText("Autocorrect", enabled), b -> {
            enabled = !enabled;
            b.setMessage(toggleText("Autocorrect", enabled));
        }).dimensions(left, TOP, colWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleText("Spell check", spellCheck), b -> {
            spellCheck = !spellCheck;
            b.setMessage(toggleText("Spell check", spellCheck));
        }).dimensions(right, TOP, colWidth, 20).build());

        // Word lists
        fixesBox = Compat.editBox(this.textRenderer, left, BOX_Y, colWidth, boxHeight,
                Text.literal("e.g.  gn = good night"), Text.literal("My fixes"));
        fixesBox.setMaxLength(20000);
        fixesBox.setText(fixesText);
        fixesBox.setChangeListener(t -> fixesText = t);
        addDrawableChild(fixesBox);

        neverChangeBox = Compat.editBox(this.textRenderer, right, BOX_Y, colWidth, boxHeight,
                Text.literal("e.g.  your nickname"), Text.literal("Never change"));
        neverChangeBox.setMaxLength(20000);
        neverChangeBox.setText(neverChangeText);
        neverChangeBox.setChangeListener(t -> neverChangeText = t);
        addDrawableChild(neverChangeBox);

        // Save / cancel
        int buttonsY = this.height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveAndClose())
                .dimensions(this.width / 2 - 154, buttonsY, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, b -> close())
                .dimensions(this.width / 2 + 4, buttonsY, 150, 20).build());
    }

    private void saveAndClose() {
        Config.enabled = enabled;
        Config.spellCheck = spellCheck;
        Config.fixes = WordLists.parseFixes(fixesText);
        Config.neverChange = WordLists.parseWords(neverChangeText);
        Config.apply();
        Config.save();
        close();
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF); // ARGB: newer versions need the alpha byte

        int gap = 10;
        int colWidth = Math.min(220, (this.width - 3 * gap) / 2);
        int left = this.width / 2 - colWidth - gap / 2;
        int right = this.width / 2 + gap / 2;
        context.drawTextWithShadow(this.textRenderer, Text.literal("My fixes ").append(
                Text.literal("(wrong = right)").formatted(Formatting.GRAY)), left, LABEL_Y, 0xFFFFFFFF); // ARGB: newer versions need the alpha byte
        context.drawTextWithShadow(this.textRenderer, Text.literal("Never change ").append(
                Text.literal("(one per line)").formatted(Formatting.GRAY)), right, LABEL_Y, 0xFFFFFFFF); // ARGB: newer versions need the alpha byte
    }

    private static Text toggleText(String name, boolean on) {
        return Text.literal(name + ": ").append(on
                ? Text.literal("ON").formatted(Formatting.GREEN)
                : Text.literal("OFF").formatted(Formatting.RED));
    }
}
