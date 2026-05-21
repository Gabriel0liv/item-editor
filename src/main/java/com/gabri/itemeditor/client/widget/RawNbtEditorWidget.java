package com.gabri.itemeditor.client.widget;

import com.gabri.babel.core.client.ui.BabelRect;
import com.gabri.babel.core.client.ui.BabelSize;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

public class RawNbtEditorWidget extends BabelWidget {
    private final MultiLineEditBox editor;

    public RawNbtEditorWidget(Font font, String initialValue) {
        this.editor = new MultiLineEditBox(font, 0, 0, 10, 10, Component.translatable("itemeditor.ui.raw_nbt"), Component.empty());
        this.editor.setValue(initialValue == null ? "" : initialValue);
        style().fillWidth();
        style().padding(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);
    }

    public String value() {
        return this.editor.getValue();
    }

    public void value(String value) {
        this.editor.setValue(value == null ? "" : value);
    }

    @Override
    public BabelSize measure(Font font, int availableWidth, int availableHeight) {
        return resolveOuterSize(200, 260, availableWidth, availableHeight);
    }

    @Override
    public void layout(Font font, BabelRect bounds) {
        super.layout(font, bounds);
        BabelRect content = contentBounds();
        this.editor.setX(content.x());
        this.editor.setY(content.y());
        this.editor.setWidth(content.width());
        this.editor.setHeight(content.height());
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        drawBackground(graphics);
        this.editor.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void updatePointer(double mouseX, double mouseY) {
        super.updatePointer(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.editor.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.editor.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.editor.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.editor.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.editor.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.editor.charTyped(codePoint, modifiers);
    }

    @Override
    public void clearFocus() {
        this.editor.setFocused(false);
    }
}
