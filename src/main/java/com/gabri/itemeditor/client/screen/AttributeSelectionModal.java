package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.itemeditor.client.widget.AttributeChoiceWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AttributeSelectionModal extends BabelPanel {
    private final Consumer<String> onSelect;
    private final Runnable onCancel;
    private final List<Attribute> allAttributes = new ArrayList<>();
    private final List<Attribute> filteredAttributes = new ArrayList<>();
    private final BabelTextField searchField;
    private final BabelScrollPane listPane;

    public AttributeSelectionModal(Font font, Consumer<String> onSelect, Runnable onCancel) {
        this.onSelect = onSelect == null ? value -> {} : onSelect;
        this.onCancel = onCancel == null ? () -> {} : onCancel;

        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            if (attribute != null) {
                allAttributes.add(attribute);
            }
        }
        allAttributes.sort(Comparator.comparing(AttributeSelectionModal::attributeIdString, String.CASE_INSENSITIVE_ORDER));
        filteredAttributes.addAll(allAttributes);

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

        BabelLabel title = new BabelLabel(Component.literal("Select Attribute"));
        title.style().textColor(BabelTheme.TEXT);
        add(title);

        this.searchField = new BabelTextField("", value -> {
            filter(value);
            rebuild();
        });
        this.searchField.placeholder(Component.literal("Search attributes"));
        this.searchField.maxLength(64).fillWidth();
        add(this.searchField);

        this.listPane = new BabelScrollPane().fillWidth().height(listHeight);
        this.listPane.showScrollbar(true);
        add(this.listPane);

        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(new BabelButton(Component.literal("Cancel"), this.onCancel).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        add(actions);

        rebuild();
    }

    private void filter(String query) {
        filteredAttributes.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Attribute attribute : allAttributes) {
            String id = attributeIdString(attribute).toLowerCase(Locale.ROOT);
            String display = Component.translatable(attribute.getDescriptionId()).getString().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || id.contains(normalized) || display.contains(normalized)) {
                filteredAttributes.add(attribute);
            }
        }
    }

    private void rebuild() {
        listPane.clear();
        if (filteredAttributes.isEmpty()) {
            listPane.add(BabelTemplates.emptyState(Component.literal("No attributes"), Component.literal("No matching registry entries were found.")));
            return;
        }
        for (Attribute attribute : filteredAttributes) {
            String id = attributeIdString(attribute);
            String name = Component.translatable(attribute.getDescriptionId()).getString();
            AttributeChoiceWidget row = new AttributeChoiceWidget(Component.literal(name), Component.literal(id), () -> {
                onSelect.accept(id);
            });
            listPane.add(row);
        }
    }

    private static String attributeIdString(Attribute attribute) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id == null ? "unknown" : id.toString();
    }
}
