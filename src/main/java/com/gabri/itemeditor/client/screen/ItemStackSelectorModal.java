package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.itemeditor.client.widget.ItemStackChoiceWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ItemStackSelectorModal extends BabelPanel {
    private final Consumer<ItemStack> onSelect;
    private final Runnable onCancel;
    private final List<ItemEntry> allEntries = new ArrayList<>();
    private final List<ItemEntry> filteredEntries = new ArrayList<>();
    private final BabelTextField searchField;
    private final BabelScrollPane listPane;

    public ItemStackSelectorModal(Font font, Consumer<ItemStack> onSelect, Runnable onCancel) {
        this.onSelect = onSelect == null ? stack -> {} : onSelect;
        this.onCancel = onCancel == null ? () -> {} : onCancel;

        Inventory inventory = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getInventory() : null;
        if (inventory != null) {
            for (int i = 0; i < inventory.items.size(); i++) {
                ItemStack stack = inventory.items.get(i);
                if (!stack.isEmpty()) {
                    allEntries.add(new ItemEntry("Inv " + i, stack.copy()));
                }
            }
            for (int i = 0; i < inventory.armor.size(); i++) {
                ItemStack stack = inventory.armor.get(i);
                if (!stack.isEmpty()) {
                    allEntries.add(new ItemEntry("Armor " + i, stack.copy()));
                }
            }
            for (int i = 0; i < inventory.offhand.size(); i++) {
                ItemStack stack = inventory.offhand.get(i);
                if (!stack.isEmpty()) {
                    allEntries.add(new ItemEntry("Offhand " + i, stack.copy()));
                }
            }
        }
        filteredEntries.addAll(allEntries);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int modalWidth = Math.max(320, Math.min(520, screenWidth - 32));
        int listHeight = Math.max(120, Math.min(220, screenHeight - 128));

        style().width(modalWidth);
        style().autoHeight();
        style().padding(10);
        style().gap(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);

        BabelLabel title = new BabelLabel(Component.translatable("itemeditor.ui.select_item_from_inventory"));
        title.style().textColor(BabelTheme.TEXT);
        add(title);

        this.searchField = new BabelTextField("", value -> {
            filter(value);
            rebuild();
        });
        this.searchField.placeholder(Component.translatable("itemeditor.ui.search_items"));
        this.searchField.maxLength(64).fillWidth();
        add(this.searchField);

        this.listPane = new BabelScrollPane().fillWidth().height(listHeight);
        this.listPane.showScrollbar(true);
        add(this.listPane);

        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.cancel"), this.onCancel).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        add(actions);

        rebuild();
    }

    private void filter(String query) {
        filteredEntries.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (ItemEntry entry : allEntries) {
            String id = itemId(entry.stack());
            String name = entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || name.contains(normalized) || id.contains(normalized)) {
                filteredEntries.add(entry);
            }
        }
    }

    private void rebuild() {
        listPane.clear();
        if (filteredEntries.isEmpty()) {
            listPane.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_items"), Component.translatable("itemeditor.ui.no_items_desc")));
            return;
        }
        for (ItemEntry entry : filteredEntries) {
            ItemStackChoiceWidget row = new ItemStackChoiceWidget(entry.stack(), Component.literal(entry.stack().getHoverName().getString()), Component.literal(entry.slotLabel()), () -> {
                onSelect.accept(entry.stack().copy());
            });
            listPane.add(row);
        }
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return ForgeRegistries.ITEMS.getKey(stack.getItem()) != null ? ForgeRegistries.ITEMS.getKey(stack.getItem()).toString() : "";
    }

    private record ItemEntry(String slotLabel, ItemStack stack) {
    }
}
