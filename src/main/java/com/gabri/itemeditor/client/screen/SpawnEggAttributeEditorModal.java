package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.itemeditor.client.widget.AttributeChoiceWidget;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import com.gabri.itemeditor.editor.ItemEditorCompat.SpawnEggAttributeEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class SpawnEggAttributeEditorModal extends BabelPanel {
    private final Consumer<SpawnEggAttributeEntry> onSave;
    private final Runnable onCancel;
    private final int entryIndex;

    private final List<Attribute> allAttributes = new ArrayList<>();
    private final List<Attribute> filteredAttributes = new ArrayList<>();

    private final BabelTextField idField;
    private final BabelTextField baseField;
    private final BabelTextField searchField;
    private final BabelScrollPane selectorList;

    private boolean selectingAttribute;
    private String attributeQuery = "";

    public SpawnEggAttributeEditorModal(Font font, SpawnEggAttributeEntry existing,
                                        Consumer<SpawnEggAttributeEntry> onSave, Runnable onCancel) {
        this.onSave = onSave == null ? entry -> {} : onSave;
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        this.entryIndex = existing != null ? existing.index() : -1;

        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            if (attribute != null) {
                this.allAttributes.add(attribute);
            }
        }
        this.allAttributes.sort(Comparator.comparing(SpawnEggAttributeEditorModal::attributeIdString,
                String.CASE_INSENSITIVE_ORDER));
        this.filteredAttributes.addAll(this.allAttributes);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int modalWidth = Math.max(300, Math.min(440, screenWidth - 32));
        int selectorHeight = Math.max(96, Math.min(180, screenHeight - 128));

        style().width(modalWidth);
        style().autoHeight();
        style().padding(10);
        style().gap(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);

        this.idField = new BabelTextField(existing != null ? existing.attributeName() : "", value -> {});
        this.idField.maxLength(128).fillWidth();

        this.baseField = new BabelTextField(existing != null ? Double.toString(existing.base()) : "0.0", value -> {});
        this.baseField.maxLength(32).inputFilter(value -> value.isEmpty() || value.matches("[-0-9.]+")).fillWidth();

        this.searchField = new BabelTextField("", value -> {
            this.attributeQuery = value == null ? "" : value;
            filterAttributes(this.attributeQuery);
            rebuildSelectorList();
        });
        this.searchField.maxLength(64).fillWidth();
        this.searchField.placeholder(Component.literal("Search attributes"));

        this.selectorList = new BabelScrollPane().fillWidth().height(selectorHeight);
        this.selectorList.showScrollbar(true);

        rebuild();
    }

    private void rebuild() {
        clear();

        add(new BabelLabel(Component.literal(this.selectingAttribute ? "Select Attribute" : "Edit Mob Attribute")));
        if (this.selectingAttribute) {
            BabelRow toolbar = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
            toolbar.add(this.searchField.fillWidth());
            toolbar.add(new BabelButton(Component.literal("Back"), this::closeSelector)
                    .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
            add(toolbar);
            add(this.selectorList);
            rebuildSelectorList();
            add(actionsRow(false));
            return;
        }

        BabelRow attributeRow = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        attributeRow.add(this.idField.fillWidth());
        attributeRow.add(new BabelButton(Component.literal("Search"), this::openSelector)
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        add(BabelTemplates.settingsRow(Component.literal("Attribute ID"), attributeRow));
        add(BabelTemplates.settingsRow(Component.literal("Base"), this.baseField));
        add(actionsRow(true));
    }

    private BabelWidget actionsRow(boolean mainForm) {
        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        if (mainForm) {
            actions.add(new BabelButton(Component.literal("Save"), this::save)
                            .colors(BabelTheme.SUCCESS, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT),
                    new BabelButton(Component.literal("Cancel"), this.onCancel)
                            .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        } else {
            actions.add(new BabelButton(Component.literal("Cancel"), this.onCancel)
                    .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        }
        return actions;
    }

    private void openSelector() {
        this.selectingAttribute = true;
        this.attributeQuery = "";
        this.searchField.setValueSilently("");
        filterAttributes(this.attributeQuery);
        rebuild();
    }

    private void closeSelector() {
        this.selectingAttribute = false;
        rebuild();
    }

    private void filterAttributes(String query) {
        this.filteredAttributes.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Attribute attribute : this.allAttributes) {
            String id = attributeIdString(attribute).toLowerCase(Locale.ROOT);
            String display = Component.translatable(attribute.getDescriptionId()).getString().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || id.contains(normalized) || display.contains(normalized)) {
                this.filteredAttributes.add(attribute);
            }
        }
    }

    private void rebuildSelectorList() {
        this.selectorList.clear();
        if (this.filteredAttributes.isEmpty()) {
            this.selectorList.add(BabelTemplates.emptyState(Component.literal("No attributes"),
                    Component.literal("No matching registry entries were found.")));
            return;
        }

        String selected = ItemEditorCompat.normalizeId(this.idField.value());
        for (Attribute attribute : this.filteredAttributes) {
            String id = attributeIdString(attribute);
            String name = Component.translatable(attribute.getDescriptionId()).getString();
            AttributeChoiceWidget choice = new AttributeChoiceWidget(Component.literal(name), Component.literal(id), () -> {
                this.idField.setValueSilently(id);
                this.selectingAttribute = false;
                rebuild();
            }).selected(id.equalsIgnoreCase(selected));
            this.selectorList.add(choice.fillWidth());
        }
    }

    private void save() {
        String attribute = ItemEditorCompat.normalizeId(this.idField.value());
        if (attribute.isEmpty()) {
            return;
        }

        double base;
        try {
            base = Double.parseDouble(this.baseField.value().trim());
        } catch (Exception ignored) {
            base = 0.0D;
        }

        this.onSave.accept(new SpawnEggAttributeEntry(attribute, base, this.entryIndex));
    }

    private static String attributeIdString(Attribute attribute) {
        var id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id == null ? "unknown" : id.toString();
    }
}
