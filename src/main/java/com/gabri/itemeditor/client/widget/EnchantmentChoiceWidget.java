package com.gabri.itemeditor.client.widget;

import com.gabri.babel.core.client.ui.BabelSize;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class EnchantmentChoiceWidget extends BabelWidget {
    private final Component title;
    private final Component subtitle;
    private final Runnable onSelect;
    private boolean selected;

    public EnchantmentChoiceWidget(Component title, Component subtitle, Runnable onSelect) {
        this.title = title == null ? Component.empty() : title;
        this.subtitle = subtitle == null ? Component.empty() : subtitle;
        this.onSelect = onSelect == null ? () -> {} : onSelect;
        style().fillWidth();
        style().height(28);
        style().padding(4, 6);
        style().background(0x661F1F1F);
        style().border(BabelTheme.BORDER, 1);
    }

    public EnchantmentChoiceWidget selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public BabelSize measure(Font font, int availableWidth, int availableHeight) {
        return resolveOuterSize(0, 28, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int background = this.selected ? 0x883B6CF6 : (hovered() ? 0x882A2A2A : 0x661F1F1F);
        graphics.fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), background);
        if (style().borderVisible()) {
            int color = style().borderColor();
            int bw = style().borderWidth();
            graphics.fill(bounds().x(), bounds().y(), bounds().right(), bounds().y() + bw, color);
            graphics.fill(bounds().x(), bounds().bottom() - bw, bounds().right(), bounds().bottom(), color);
            graphics.fill(bounds().x(), bounds().y(), bounds().x() + bw, bounds().bottom(), color);
            graphics.fill(bounds().right() - bw, bounds().y(), bounds().right(), bounds().bottom(), color);
        }

        graphics.drawString(font, title, bounds().x() + 6, bounds().y() + 4, BabelTheme.TEXT, false);
        if (!subtitle.getString().isEmpty()) {
            graphics.drawString(font, subtitle, bounds().x() + 6, bounds().y() + 15, BabelTheme.TEXT_MUTED, false);
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
