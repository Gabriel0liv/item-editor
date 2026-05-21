package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.babel.core.util.BabelCuriosSupport;
import com.gabri.itemeditor.client.widget.AttributeChoiceWidget;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import com.gabri.itemeditor.editor.ItemEditorCompat.AttributeEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AttributeEditorModal extends BabelPanel {
    private enum SelectorMode {
        NONE,
        ATTRIBUTE,
        SLOT
    }

    private final Consumer<AttributeEntry> onSave;
    private final Runnable onCancel;

    private final List<Attribute> allAttributes = new ArrayList<>();
    private final List<Attribute> filteredAttributes = new ArrayList<>();
    private final List<String> allSlots = new ArrayList<>();
    private final List<String> filteredSlots = new ArrayList<>();

    private final BabelTextField idField;
    private final BabelTextField amountField;
    private final BabelButton operationButton;
    private final BabelButton attributeSelectButton;
    private final BabelButton slotSelectButton;
    private final BabelButton saveButton;
    private final BabelButton cancelButton;
    private final BabelTextField selectorSearchField;
    private final BabelScrollPane selectorList;

    private SelectorMode selectorMode = SelectorMode.NONE;
    private int operationIndex;
    private String selectedSlot;
    private String attributeQuery = "";
    private String slotQuery = "";

    public AttributeEditorModal(Font font, AttributeEntry existing, Consumer<AttributeEntry> onSave, Runnable onCancel) {
        this.onSave = onSave == null ? entry -> {} : onSave;
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        this.operationIndex = existing != null ? existing.operation() : 0;
        this.selectedSlot = existing != null && existing.slot() != null && !existing.slot().isBlank() ? existing.slot().trim() : "any";

        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            if (attribute != null) {
                allAttributes.add(attribute);
            }
        }
        allAttributes.sort(Comparator.comparing(AttributeEditorModal::attributeIdString, String.CASE_INSENSITIVE_ORDER));
        filteredAttributes.addAll(allAttributes);

        allSlots.addAll(ItemEditorCompat.vanillaSlotOptions());
        if (ModList.get().isLoaded("curios")) {
            BabelCuriosSupport.getCuriosSlotIds(Minecraft.getInstance().player).forEach(slot -> {
                if (!allSlots.contains(slot)) {
                    allSlots.add(slot);
                }
            });
        }
        allSlots.sort((left, right) -> {
            if ("any".equalsIgnoreCase(left)) {
                return "any".equalsIgnoreCase(right) ? 0 : -1;
            }
            if ("any".equalsIgnoreCase(right)) {
                return 1;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(left, right);
        });
        filteredSlots.addAll(allSlots);

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

        this.idField = new BabelTextField(existing != null ? existing.attributeName() : "", v -> {});
        this.idField.maxLength(128).fillWidth();

        this.amountField = new BabelTextField(existing != null ? Double.toString(existing.amount()) : "0.0", v -> {});
        this.amountField.maxLength(32).inputFilter(value -> value.isEmpty() || value.matches("[-0-9.]+")).fillWidth();

        this.operationButton = new BabelButton(Component.literal(operationLabel()), this::cycleOperation);
        this.operationButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);

        this.attributeSelectButton = new BabelButton(Component.literal("Search"), this::openAttributeSelector);
        this.attributeSelectButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);

        this.slotSelectButton = new BabelButton(Component.literal(slotLabel(this.selectedSlot)), this::openSlotSelector);
        this.slotSelectButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);

        this.saveButton = new BabelButton(Component.literal("Save"), this::save);
        this.saveButton.colors(BabelTheme.SUCCESS, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT);

        this.cancelButton = new BabelButton(Component.literal("Cancel"), this.onCancel);
        this.cancelButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);

        this.selectorSearchField = new BabelTextField("", value -> {
            if (this.selectorMode == SelectorMode.ATTRIBUTE) {
                this.attributeQuery = value == null ? "" : value;
                filterAttributes(this.attributeQuery);
            } else if (this.selectorMode == SelectorMode.SLOT) {
                this.slotQuery = value == null ? "" : value;
                filterSlots(this.slotQuery);
            }
            rebuild();
        });
        this.selectorSearchField.maxLength(64).fillWidth();
        this.selectorSearchField.placeholder(Component.literal("Search"));

        this.selectorList = new BabelScrollPane().fillWidth().height(selectorHeight);
        this.selectorList.showScrollbar(true);

        rebuild();
    }

    private void rebuild() {
        clear();

        if (this.selectorMode == SelectorMode.ATTRIBUTE) {
            add(selectorTitle("Select Attribute"));
            add(selectorToolbar(this.attributeQuery));
            add(this.selectorList);
            rebuildAttributeList();
            add(actionsRow(false));
            return;
        }

        if (this.selectorMode == SelectorMode.SLOT) {
            add(selectorTitle("Select Slot"));
            add(selectorToolbar(this.slotQuery));
            add(this.selectorList);
            rebuildSlotList();
            add(actionsRow(false));
            return;
        }

        add(new BabelLabel(Component.literal("Edit Attribute")));
        add(attributeRow());
        add(valueRow());
        add(slotRow());
        add(actionsRow(true));
    }

    private BabelWidget selectorTitle(String text) {
        BabelLabel title = new BabelLabel(Component.literal(text));
        title.style().textColor(BabelTheme.TEXT);
        return title;
    }

    private BabelWidget selectorToolbar(String query) {
        this.selectorSearchField.setValueSilently(query == null ? "" : query);
        BabelRow toolbar = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        toolbar.add(this.selectorSearchField.fillWidth());
        toolbar.add(new BabelButton(Component.literal("Back"), this::closeSelector).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        return toolbar;
    }

    private BabelWidget attributeRow() {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        row.add(this.idField.fillWidth());
        row.add(this.attributeSelectButton);
        return BabelTemplates.settingsRow(Component.literal("Attribute ID"), row);
    }

    private BabelWidget valueRow() {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        row.add(this.amountField.fillWidth());
        row.add(this.operationButton);
        return BabelTemplates.settingsRow(Component.literal("Value"), row);
    }

    private BabelWidget slotRow() {
        return BabelTemplates.settingsRow(Component.literal("Slot"), this.slotSelectButton.fillWidth());
    }

    private BabelWidget actionsRow(boolean mainForm) {
        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        if (mainForm) {
            actions.add(this.saveButton, this.cancelButton);
        } else {
            actions.add(this.cancelButton);
        }
        return actions;
    }

    private void openAttributeSelector() {
        this.selectorMode = SelectorMode.ATTRIBUTE;
        this.attributeQuery = "";
        filterAttributes(this.attributeQuery);
        rebuild();
    }

    private void openSlotSelector() {
        this.selectorMode = SelectorMode.SLOT;
        this.slotQuery = "";
        filterSlots(this.slotQuery);
        rebuild();
    }

    private void closeSelector() {
        this.selectorMode = SelectorMode.NONE;
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

    private void filterSlots(String query) {
        this.filteredSlots.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (String slot : this.allSlots) {
            if (normalized.isEmpty() || slot.toLowerCase(Locale.ROOT).contains(normalized)) {
                this.filteredSlots.add(slot);
            }
        }
        if (!this.filteredSlots.contains(this.selectedSlot) && !this.selectedSlot.isBlank()) {
            this.filteredSlots.add(this.selectedSlot);
        }
    }

    private void rebuildAttributeList() {
        this.selectorList.clear();
        if (this.filteredAttributes.isEmpty()) {
            this.selectorList.add(BabelTemplates.emptyState(Component.literal("No attributes"), Component.literal("No matching registry entries were found.")));
            return;
        }

        for (Attribute attribute : this.filteredAttributes) {
            String id = attributeIdString(attribute);
            String name = Component.translatable(attribute.getDescriptionId()).getString();
            AttributeChoiceWidget choice = new AttributeChoiceWidget(Component.literal(name), Component.literal(id), () -> {
                this.idField.setValueSilently(id);
                this.selectorMode = SelectorMode.NONE;
                rebuild();
            }).selected(id.equalsIgnoreCase(ItemEditorCompat.normalizeId(this.idField.value())));
            this.selectorList.add(choice.fillWidth());
        }
    }

    private void rebuildSlotList() {
        this.selectorList.clear();
        if (this.filteredSlots.isEmpty()) {
            this.selectorList.add(BabelTemplates.emptyState(Component.literal("No slots"), Component.literal("No matching slots were found.")));
            return;
        }

        for (String slot : this.filteredSlots) {
            boolean selected = slot.equalsIgnoreCase(this.selectedSlot);
            AttributeChoiceWidget choice = new AttributeChoiceWidget(
                    Component.literal(slot),
                    Component.literal(isCurioSlot(slot) ? "Curios slot" : "Vanilla slot"),
                    () -> {
                        this.selectedSlot = slot;
                        this.slotSelectButton.text(Component.literal(slotLabel(slot)));
                        this.selectorMode = SelectorMode.NONE;
                        rebuild();
                    }).selected(selected);
            this.selectorList.add(choice.fillWidth());
        }
    }

    private void cycleOperation() {
        this.operationIndex = (this.operationIndex + 1) % 3;
        this.operationButton.text(Component.literal(operationLabel()));
    }

    private String operationLabel() {
        return switch (this.operationIndex) {
            case 1 -> "MULTIPLY_BASE";
            case 2 -> "MULTIPLY_TOTAL";
            default -> "ADDITION";
        };
    }

    private void save() {
        String attribute = ItemEditorCompat.normalizeId(this.idField.value());
        if (attribute.isEmpty()) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(this.amountField.value().trim());
        } catch (Exception ignored) {
            amount = 0.0D;
        }

        String slot = this.selectedSlot == null || this.selectedSlot.isBlank() ? "any" : this.selectedSlot.trim();
        boolean curio = isCurioSlot(slot);
        AttributeEntry result = new AttributeEntry(attribute, amount, this.operationIndex, slot, curio, java.util.UUID.randomUUID(), -1);
        this.onSave.accept(result);
    }

    private static boolean isCurioSlot(String slot) {
        if (slot == null || slot.isBlank()) {
            return false;
        }
        String normalized = slot.trim().toLowerCase(Locale.ROOT);
        if ("any".equals(normalized)) {
            return false;
        }
        for (String vanillaSlot : ItemEditorCompat.vanillaSlotOptions()) {
            if (vanillaSlot.equalsIgnoreCase(normalized)) {
                return false;
            }
        }
        return true;
    }

    private static String slotLabel(String slot) {
        if (slot == null || slot.isBlank()) {
            return "any";
        }
        return slot;
    }

    private static String attributeIdString(Attribute attribute) {
        var id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id == null ? "unknown" : id.toString();
    }
}
