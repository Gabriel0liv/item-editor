package com.gabri.itemeditor.client.widget;

import com.gabri.babel.core.api.Babel;
import com.gabri.babel.core.client.ui.BabelRect;
import com.gabri.babel.core.client.ui.BabelSize;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelWidget;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ItemStackPreviewWidget extends BabelWidget {
    private final Supplier<ItemStack> stackSupplier;

    public ItemStackPreviewWidget(Supplier<ItemStack> stackSupplier) {
        this.stackSupplier = stackSupplier == null ? () -> ItemStack.EMPTY : stackSupplier;
        style().fillWidth();
        style().padding(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);
    }

    @Override
    public BabelSize measure(Font font, int availableWidth, int availableHeight) {
        return resolveOuterSize(220, 74, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        drawBackground(graphics);
        ItemStack stack = stackSupplier.get();
        BabelRect content = contentBounds();
        int iconSize = 32;
        int iconX = content.x() + 8;
        int iconY = content.y() + 8;

        if (stack != null && !stack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(iconX + iconSize / 2.0F, iconY + iconSize / 2.0F, 0.0F);
            graphics.pose().scale(2.0F, 2.0F, 1.0F);
            graphics.pose().translate(-8.0F, -8.0F, 0.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
            graphics.pose().popPose();
        } else {
                graphics.drawString(font, Component.translatable("itemeditor.ui.empty_slot"), iconX + 2, iconY + 10, BabelTheme.TEXT_MUTED, false);
        }

        int textX = iconX + iconSize + 10;
        int textY = content.y() + 8;
        Component nameText = stack == null || stack.isEmpty() ? Component.translatable("itemeditor.ui.empty_slot") : stack.getHoverName();
        Component idText = Component.translatable("itemeditor.ui.id_value", Babel.itemStack(stack).itemId());
        Component countText = Component.translatable("itemeditor.ui.count_value", stack == null || stack.isEmpty() ? 0 : stack.getCount());
        Component damageText = Component.translatable("itemeditor.ui.damage_value", stack != null && stack.isDamageableItem() ? stack.getDamageValue() : 0);

        graphics.drawString(font, nameText, textX, textY, BabelTheme.TEXT, false);
        graphics.drawString(font, idText, textX, textY + 12, BabelTheme.TEXT_MUTED, false);
        graphics.drawString(font, countText, textX, textY + 22, BabelTheme.TEXT_MUTED, false);
        graphics.drawString(font, damageText, textX, textY + 32, BabelTheme.TEXT_MUTED, false);
    }
}
