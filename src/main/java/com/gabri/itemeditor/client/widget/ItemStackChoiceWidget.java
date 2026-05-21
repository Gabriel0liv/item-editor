package com.gabri.itemeditor.client.widget;

import com.gabri.babel.core.client.ui.BabelSize;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemStackChoiceWidget extends BabelWidget {
    private final ItemStack stack;
    private final Component title;
    private final Component subtitle;
    private final Runnable onSelect;

    public ItemStackChoiceWidget(ItemStack stack, Component title, Component subtitle, Runnable onSelect) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.title = title == null ? Component.empty() : title;
        this.subtitle = subtitle == null ? Component.empty() : subtitle;
        this.onSelect = onSelect == null ? () -> {} : onSelect;
        style().fillWidth();
        style().height(30);
        style().padding(4, 6);
        style().background(0x661F1F1F);
        style().border(BabelTheme.BORDER, 1);
    }

    @Override
    public BabelSize measure(Font font, int availableWidth, int availableHeight) {
        return resolveOuterSize(0, 30, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int background = hovered() ? 0x882A2A2A : 0x661F1F1F;
        graphics.fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), background);
        if (style().borderVisible()) {
            int color = style().borderColor();
            int bw = style().borderWidth();
            graphics.fill(bounds().x(), bounds().y(), bounds().right(), bounds().y() + bw, color);
            graphics.fill(bounds().x(), bounds().bottom() - bw, bounds().right(), bounds().bottom(), color);
            graphics.fill(bounds().x(), bounds().y(), bounds().x() + bw, bounds().bottom(), color);
            graphics.fill(bounds().right() - bw, bounds().y(), bounds().right(), bounds().bottom(), color);
        }

        int iconX = bounds().x() + 6;
        int iconY = bounds().y() + 6;
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, iconX, iconY);
        }

        int textX = iconX + 20;
        graphics.drawString(font, title, textX, bounds().y() + 4, BabelTheme.TEXT, false);
        if (!subtitle.getString().isEmpty()) {
            graphics.drawString(font, subtitle, textX, bounds().y() + 15, BabelTheme.TEXT_MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInside(mouseX, mouseY)) {
            setActive(true);
            onSelect.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && active()) {
            setActive(false);
            return true;
        }
        return false;
    }
}
