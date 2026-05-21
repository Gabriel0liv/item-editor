package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.BabelJustify;
import com.gabri.babel.core.client.ui.BabelPanel;
import com.gabri.babel.core.client.ui.BabelRow;
import com.gabri.babel.core.client.ui.BabelScrollPane;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelTextField;
import com.gabri.babel.core.client.ui.BabelWidget;
import com.gabri.babel.core.client.ui.BabelTemplates;
import com.gabri.itemeditor.client.widget.EnchantmentChoiceWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EnchantmentSelectionModal extends BabelPanel {
    private final Consumer<String> onSelect;
    private final Runnable onCancel;
    private final List<Enchantment> allEnchantments = new ArrayList<>();
    private final List<Enchantment> filteredEnchantments = new ArrayList<>();
    private final BabelTextField searchField;
    private final BabelScrollPane listPane;
    private final String selectedId;
    private String query = "";

    public EnchantmentSelectionModal(Font font, String selectedId, Consumer<String> onSelect, Runnable onCancel) {
        this.onSelect = onSelect == null ? value -> {} : onSelect;
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        this.selectedId = selectedId == null ? "" : selectedId.trim();

        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValues()) {
            if (enchantment != null) {
                allEnchantments.add(enchantment);
            }
        }
        allEnchantments.sort(Comparator.comparing(EnchantmentSelectionModal::enchantmentIdString, String.CASE_INSENSITIVE_ORDER));
        filteredEnchantments.addAll(allEnchantments);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int modalWidth = Math.max(300, Math.min(460, screenWidth - 32));
        int listHeight = Math.max(96, Math.min(180, screenHeight - 128));

        style().width(modalWidth);
        style().autoHeight();
        style().padding(10);
        style().gap(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);

        add(title());

        this.searchField = new BabelTextField("", value -> {
            this.query = value == null ? "" : value;
            filter(this.query);
            rebuild();
        });
        this.searchField.placeholder(Component.translatable("itemeditor.ui.search_enchantments"));
        this.searchField.maxLength(64).fillWidth();
        add(this.searchField);

        this.listPane = new BabelScrollPane().fillWidth().height(listHeight);
        this.listPane.showScrollbar(true);
        add(this.listPane);

        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(new com.gabri.babel.core.client.ui.BabelButton(Component.translatable("itemeditor.ui.cancel"), this.onCancel).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        add(actions);

        rebuild();
    }

    private BabelWidget title() {
        BabelRow row = new BabelRow().fillWidth().align(com.gabri.babel.core.client.ui.BabelAlign.CENTER);
        com.gabri.babel.core.client.ui.BabelLabel label = new com.gabri.babel.core.client.ui.BabelLabel(Component.translatable("itemeditor.ui.select_enchantment"));
        label.style().textColor(BabelTheme.TEXT);
        row.add(label);
        return row;
    }

    private void filter(String query) {
        this.filteredEnchantments.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Enchantment enchantment : this.allEnchantments) {
            String id = enchantmentIdString(enchantment).toLowerCase(Locale.ROOT);
            String display = Component.translatable(enchantment.getDescriptionId()).getString().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || id.contains(normalized) || display.contains(normalized)) {
                this.filteredEnchantments.add(enchantment);
            }
        }
    }

    private void rebuild() {
        this.listPane.clear();
        if (this.filteredEnchantments.isEmpty()) {
            this.listPane.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_enchantments"), Component.empty()));
            return;
        }
        for (Enchantment enchantment : this.filteredEnchantments) {
            String id = enchantmentIdString(enchantment);
            String name = Component.translatable(enchantment.getDescriptionId()).getString();
            EnchantmentChoiceWidget row = new EnchantmentChoiceWidget(Component.literal(name), Component.literal(id), () -> this.onSelect.accept(id));
            row.selected(id.equalsIgnoreCase(this.selectedId));
            this.listPane.add(row);
        }
    }

    private static String enchantmentIdString(Enchantment enchantment) {
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return id == null ? "unknown" : id.toString();
    }
}
