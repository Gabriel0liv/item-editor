package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.babel.core.util.BabelCuriosSupport;
import com.gabri.itemeditor.client.widget.AttributeChoiceWidget;
import com.gabri.itemeditor.client.widget.EffectChoiceWidget;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import com.gabri.itemeditor.editor.ItemEditorCompat.EffectCategory;
import com.gabri.itemeditor.editor.ItemEditorCompat.EffectEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EffectEditorModal extends BabelPanel {
    private enum SelectorMode {
        NONE,
        EFFECT,
        SLOT,
        COLOR
    }

    private final Consumer<EffectEntry> onSave;
    private final Runnable onCancel;
    private final EffectCategory category;
    private final EffectCategory storageCategory;
    private final boolean potionMode;
    private final int entryIndex;

    private final BabelTextField idField;
    private final BabelTextField durationField;
    private final BabelTextField amplifierField;
    private final BabelTextField chanceField;
    private final BabelButton slotButton;
    private final BabelButton colorButton;
    private final BabelButton ambientButton;
    private final BabelButton particlesButton;
    private final BabelButton iconButton;
    private final BabelButton selfButton;
    private final BabelTextField colorHexField;
    private final BabelSlider redSlider;
    private final BabelSlider greenSlider;
    private final BabelSlider blueSlider;

    private final List<MobEffect> allEffects = new ArrayList<>();
    private final List<MobEffect> filteredEffects = new ArrayList<>();
    private final List<String> allSlots = new ArrayList<>();
    private final List<String> filteredSlots = new ArrayList<>();
    private final BabelTextField searchField;
    private final BabelScrollPane selectorList;
    private SelectorMode selectorMode = SelectorMode.NONE;

    private boolean ambient;
    private boolean showParticles;
    private boolean showIcon;
    private boolean self;
    private String selectedSlot;
    private Integer selectedColor;
    private String effectQuery = "";
    private String slotQuery = "";

    public EffectEditorModal(Font font, EffectCategory category, EffectCategory storageCategory, EffectEntry existing, Consumer<EffectEntry> onSave, Runnable onCancel) {
        this(font, category, storageCategory, existing, onSave, onCancel, storageCategory != null && storageCategory.isPotion());
    }

    public EffectEditorModal(Font font, EffectCategory category, EffectCategory storageCategory, EffectEntry existing, Consumer<EffectEntry> onSave, Runnable onCancel, boolean ignoredShowColorField) {
        this.category = category == null ? EffectCategory.ON_USE : category;
        this.storageCategory = storageCategory == null ? this.category : storageCategory;
        this.potionMode = this.storageCategory.isPotion();
        this.entryIndex = existing != null ? existing.index() : -1;
        this.onSave = onSave == null ? effect -> {} : onSave;
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        this.ambient = existing != null && existing.ambient();
        this.showParticles = existing == null || existing.showParticles();
        this.showIcon = existing == null || existing.showIcon();
        this.self = existing != null && existing.self();
        this.selectedSlot = existing != null && existing.slot() != null && !existing.slot().isBlank() ? existing.slot().trim() : "any";

        for (MobEffect effect : ForgeRegistries.MOB_EFFECTS.getValues()) {
            if (effect != null) {
                allEffects.add(effect);
            }
        }
        allEffects.sort(Comparator.comparing(EffectEditorModal::effectIdString, String.CASE_INSENSITIVE_ORDER));
        filteredEffects.addAll(allEffects);

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
        int modalWidth = Math.max(300, Math.min(460, screenWidth - 32));
        int selectorHeight = Math.max(96, Math.min(180, screenHeight - 128));

        style().width(modalWidth);
        style().autoHeight();
        style().padding(10);
        style().gap(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);

        BabelLabel title = new BabelLabel(Component.literal(existing == null ? "Add Effect" : "Edit Effect"));
        title.style().textColor(BabelTheme.TEXT);
        add(title);

        this.idField = new BabelTextField(existing != null ? existing.id() : "", v -> {});
        this.idField.maxLength(128).fillWidth();

        this.searchField = new BabelTextField("", value -> {
            if (this.selectorMode == SelectorMode.SLOT) {
                this.slotQuery = value == null ? "" : value;
                filterSlots(this.slotQuery);
            } else {
                this.effectQuery = value == null ? "" : value;
                filterEffects(this.effectQuery);
            }
            rebuildSelectorList();
        });
        this.searchField.placeholder(Component.literal("Search effects"));
        this.searchField.maxLength(64).fillWidth();

        this.selectorList = new BabelScrollPane().fillWidth().height(selectorHeight);
        this.selectorList.showScrollbar(true);

        this.durationField = new BabelTextField(existing != null ? Integer.toString(existing.duration()) : "10", v -> {});
        this.durationField.maxLength(16).inputFilter(value -> value.isEmpty() || value.matches("\\d+")).fillWidth();

        this.amplifierField = new BabelTextField(existing != null ? Integer.toString(existing.amplifier()) : "0", v -> {});
        this.amplifierField.maxLength(8).inputFilter(value -> value.isEmpty() || value.matches("\\d+")).fillWidth();

        this.chanceField = new BabelTextField(existing != null ? Double.toString(existing.chance()) : "1.0", v -> {});
        this.chanceField.maxLength(16).inputFilter(value -> value.isEmpty() || value.matches("[-0-9.]+")).fillWidth();

        int initialColor = existing != null && existing.color() != null ? existing.color() : 0xFFFFFF;
        this.selectedColor = initialColor;
        this.colorHexField = new BabelTextField(String.format(Locale.ROOT, "#%06X", initialColor & 0xFFFFFF), value -> {
            Integer parsed = parseHexColor(value);
            if (parsed != null) {
                setColorValue(parsed, true);
            }
        });
        this.colorHexField.maxLength(16).fillWidth();
        this.colorHexField.inputFilter(value -> value == null || value.isEmpty() || value.matches("(?i)^#?[0-9a-f]{0,6}$"));

        int[] rgb = colorToRgb(initialColor);
        this.redSlider = new BabelSlider(rgb[0], 0, 255, value -> updateColorFromSliders());
        this.greenSlider = new BabelSlider(rgb[1], 0, 255, value -> updateColorFromSliders());
        this.blueSlider = new BabelSlider(rgb[2], 0, 255, value -> updateColorFromSliders());
        this.redSlider.fillWidth();
        this.greenSlider.fillWidth();
        this.blueSlider.fillWidth();

        this.ambientButton = stateButton("Ambient", this.ambient, this::toggleAmbient);
        this.particlesButton = stateButton("Particles", this.showParticles, this::toggleParticles);
        this.iconButton = stateButton("Icon", this.showIcon, this::toggleIcon);
        this.selfButton = stateButton("Self", this.self, this::toggleSelf);

        this.slotButton = new BabelButton(Component.literal(slotLabel(this.selectedSlot)), this::openSlotSelector);
        this.slotButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);

        this.colorButton = new BabelButton(Component.literal(colorLabel(initialColor)), this::openColorSelector);
        this.colorButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);

        rebuild();
    }

    private void rebuild() {
        clear();

        BabelLabel title = new BabelLabel(Component.literal(switch (this.selectorMode) {
            case NONE -> "Edit Effect";
            case SLOT -> "Select Slot";
            case COLOR -> "Select Color";
            case EFFECT -> "Select Effect";
        }));
        title.style().textColor(BabelTheme.TEXT);
        add(title);

        if (this.selectorMode == SelectorMode.EFFECT) {
            BabelRow toolbar = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
            toolbar.add(this.searchField.fillWidth());
            toolbar.add(new BabelButton(Component.literal("Back"), this::closeSelector).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
            add(toolbar);

            add(this.selectorList);
            rebuildSelectorList();
            add(actionsRow());
            return;
        }

        if (this.selectorMode == SelectorMode.SLOT) {
            BabelRow toolbar = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
            toolbar.add(this.searchField.fillWidth());
            toolbar.add(new BabelButton(Component.literal("Back"), this::closeSelector).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
            add(toolbar);

            add(this.selectorList);
            rebuildSlotList();
            add(actionsRow());
            return;
        }

        if (this.selectorMode == SelectorMode.COLOR) {
            add(colorPickerView());
            return;
        }

        add(effectIdRow());
        add(BabelTemplates.settingsRow(Component.literal("Duration (s)"), this.durationField));
        add(BabelTemplates.settingsRow(Component.literal("Amplifier"), this.amplifierField));

        if (this.category == EffectCategory.ON_HIT || this.category == EffectCategory.ON_HURT) {
            add(BabelTemplates.settingsRow(Component.literal("Chance"), this.chanceField));
        }

        if (this.category == EffectCategory.ON_EQUIP || this.category == EffectCategory.ON_HURT) {
            add(BabelTemplates.settingsRow(Component.literal("Slot"), this.slotButton.fillWidth()));
        }

        add(flagsGrid());

        add(actionsRow());
    }

    private BabelWidget effectIdRow() {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        row.add(this.idField.fillWidth());
        row.add(new BabelButton(Component.literal("Select"), this::openEffectSelector).colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        return BabelTemplates.settingsRow(Component.literal("Effect ID"), row);
    }

    private BabelWidget flagsGrid() {
        BabelColumn column = new BabelColumn().fillWidth().gap(6);
        column.add(flagRow(this.ambientButton, this.particlesButton));
        if (this.category == EffectCategory.ON_HIT || this.category == EffectCategory.ON_HURT) {
            column.add(flagRow(this.iconButton, this.selfButton));
        } else {
            column.add(flagRow(this.iconButton, new BabelSpacer(0, 20).fillWidth()));
        }
        return column;
    }

    private BabelWidget flagRow(BabelButton left, BabelWidget right) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        left.fillWidth();
        row.add(left);
        if (right instanceof BabelButton button) {
            button.fillWidth();
        }
        row.add(right);
        return row;
    }

    private BabelWidget actionsRow() {
        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(
                new BabelButton(Component.literal("Save"), this::save).colors(BabelTheme.SUCCESS, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT),
                new BabelButton(Component.literal("Cancel"), this.onCancel).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT)
        );
        return actions;
    }

    private void openEffectSelector() {
        this.selectorMode = SelectorMode.EFFECT;
        this.effectQuery = "";
        this.searchField.setValueSilently("");
        filterEffects(this.effectQuery);
        rebuild();
    }

    private void openSlotSelector() {
        this.selectorMode = SelectorMode.SLOT;
        this.slotQuery = "";
        this.searchField.setValueSilently("");
        filterSlots(this.slotQuery);
        rebuild();
    }

    private void openColorSelector() {
        this.selectorMode = SelectorMode.COLOR;
        rebuild();
    }

    private void closeSelector() {
        this.selectorMode = SelectorMode.NONE;
        rebuild();
    }

    private void filterEffects(String query) {
        this.filteredEffects.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (MobEffect effect : this.allEffects) {
            String id = effectIdString(effect).toLowerCase(Locale.ROOT);
            String description = effect.getDescriptionId() == null ? "" : effect.getDescriptionId().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || id.contains(normalized) || description.contains(normalized)) {
                this.filteredEffects.add(effect);
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
        if (this.selectedSlot != null && !this.selectedSlot.isBlank() && this.filteredSlots.stream().noneMatch(slot -> slot.equalsIgnoreCase(this.selectedSlot))) {
            this.filteredSlots.add(this.selectedSlot);
        }
    }

    private void rebuildSelectorList() {
        if (this.selectorList == null) {
            return;
        }

        this.selectorList.clear();
        if (this.selectorMode == SelectorMode.SLOT) {
            rebuildSlotList();
            return;
        }

        if (this.filteredEffects.isEmpty()) {
            this.selectorList.add(BabelTemplates.emptyState(Component.literal("No effects"), Component.literal("No matching mob effects were found.")));
            return;
        }

        String selected = ItemEditorCompat.normalizeId(this.idField.value());
        for (MobEffect effect : this.filteredEffects) {
            String effectId = effectIdString(effect);
            EffectChoiceWidget row = new EffectChoiceWidget(effect, effectId.equals(selected), () -> {
                this.idField.setValueSilently(effectId);
                this.selectorMode = SelectorMode.NONE;
                rebuild();
            }).fillWidth();
            this.selectorList.add(row);
        }
    }

    private void rebuildSlotList() {
        if (this.selectorList == null) {
            return;
        }

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
                        this.slotButton.text(Component.literal(slotLabel(slot)));
                        this.selectorMode = SelectorMode.NONE;
                        rebuild();
                    }).selected(selected);
            this.selectorList.add(choice.fillWidth());
        }
    }

    private void toggleAmbient() {
        this.ambient = !this.ambient;
        updateFlagButton(this.ambientButton, this.ambient);
    }

    private void toggleParticles() {
        this.showParticles = !this.showParticles;
        updateFlagButton(this.particlesButton, this.showParticles);
    }

    private void toggleIcon() {
        this.showIcon = !this.showIcon;
        updateFlagButton(this.iconButton, this.showIcon);
    }

    private void toggleSelf() {
        this.self = !this.self;
        updateFlagButton(this.selfButton, this.self);
    }

    private void save() {
        String id = ItemEditorCompat.normalizeId(this.idField.value());
        if (id.isEmpty()) {
            return;
        }

        int duration = parseInt(this.durationField.value(), 0);
        int amplifier = parseInt(this.amplifierField.value(), 0);
        double chance = (this.category == EffectCategory.ON_HIT || this.category == EffectCategory.ON_HURT) ? parseDouble(this.chanceField.value(), 1.0D) : 1.0D;
        String slot = (this.category == EffectCategory.ON_EQUIP || this.category == EffectCategory.ON_HURT)
                ? (this.selectedSlot == null || this.selectedSlot.isBlank() ? "any" : this.selectedSlot.trim())
                : "";
        boolean self = this.category == EffectCategory.ON_HIT || this.category == EffectCategory.ON_HURT ? this.self : false;
        Integer color = null;

        EffectEntry result = new EffectEntry(this.storageCategory, id, duration, amplifier, chance, self, slot, this.ambient, this.showParticles, this.showIcon, color, this.entryIndex);
        this.onSave.accept(result);
    }

    private BabelWidget colorPickerView() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow top = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        top.add(this.colorHexField.fillWidth());
        top.add(new BabelButton(Component.literal("Back"), this::closeSelector).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        column.add(top);

        column.add(colorPreview());

        column.add(BabelTemplates.settingsRow(Component.literal("Red"), this.redSlider));
        column.add(BabelTemplates.settingsRow(Component.literal("Green"), this.greenSlider));
        column.add(BabelTemplates.settingsRow(Component.literal("Blue"), this.blueSlider));

        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(new BabelButton(Component.literal("Apply"), this::applyColorSelection).colors(BabelTheme.SUCCESS, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        actions.add(new BabelButton(Component.literal("Clear"), this::clearColorSelection).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        column.add(actions);
        return column;
    }

    private BabelButton stateButton(String label, boolean selected, Runnable action) {
        BabelButton button = new BabelButton(Component.literal(label), action);
        updateFlagButton(button, selected);
        return button;
    }

    private void updateFlagButton(BabelButton button, boolean selected) {
        if (button == null) {
            return;
        }
        button.colors(selected ? 0xFF2F8F4E : 0xFF9E3C3C, selected ? 0xFF4FD67A : 0xFFC85A5A, BabelTheme.TEXT);
    }

    private void updateColorFromSliders() {
        setColorValue(rgbToColor(this.redSlider.value(), this.greenSlider.value(), this.blueSlider.value()), false);
    }

    private void setColorValue(int color, boolean updateControls) {
        this.selectedColor = color & 0xFFFFFF;
        if (updateControls) {
            int[] rgb = colorToRgb(this.selectedColor);
            this.redSlider.setValueSilently(rgb[0]);
            this.greenSlider.setValueSilently(rgb[1]);
            this.blueSlider.setValueSilently(rgb[2]);
            this.colorHexField.setValueSilently(String.format(Locale.ROOT, "#%06X", this.selectedColor));
            this.colorButton.text(Component.literal(colorLabel(this.selectedColor)));
        } else {
            this.colorHexField.setValueSilently(String.format(Locale.ROOT, "#%06X", this.selectedColor));
            this.colorButton.text(Component.literal(colorLabel(this.selectedColor)));
        }
    }

    private void applyColorSelection() {
        this.selectorMode = SelectorMode.NONE;
        rebuild();
    }

    private void clearColorSelection() {
        this.selectedColor = null;
        this.colorHexField.setValueSilently("#FFFFFF");
        this.colorButton.text(Component.literal("Pick color"));
        this.redSlider.setValueSilently(255);
        this.greenSlider.setValueSilently(255);
        this.blueSlider.setValueSilently(255);
        rebuild();
    }

    private BabelWidget colorPreview() {
        return new BabelWidget() {
            {
                style().fillWidth();
                style().height(26);
                style().background(BabelTheme.PANEL_ALT);
                style().border(BabelTheme.BORDER, 1);
            }

            @Override
            public BabelSize measure(Font font, int availableWidth, int availableHeight) {
                return resolveOuterSize(0, 26, availableWidth, availableHeight);
            }

            @Override
            public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
                drawBackground(graphics);
                BabelRect content = contentBounds();
                int color = selectedColor == null ? 0xFFFFFFFF : (0xFF000000 | (selectedColor & 0xFFFFFF));
                graphics.fill(content.x(), content.y(), content.right(), content.bottom(), color);
            }
        };
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

    private static String colorLabel(Integer color) {
        if (color == null) {
            return Component.translatable("itemeditor.ui.pick_color").getString();
        }
        return Component.translatable("itemeditor.ui.color_with_value", String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)).getString();
    }

    private static int rgbToColor(int red, int green, int blue) {
        return ((Math.max(0, Math.min(255, red)) & 0xFF) << 16)
                | ((Math.max(0, Math.min(255, green)) & 0xFF) << 8)
                | (Math.max(0, Math.min(255, blue)) & 0xFF);
    }

    private static int[] colorToRgb(int color) {
        return new int[] {
                (color >> 16) & 0xFF,
                (color >> 8) & 0xFF,
                color & 0xFF
        };
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Integer parseHexColor(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String effectIdString(MobEffect effect) {
        if (effect == null) {
            return "unknown";
        }
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id == null ? "unknown" : id.toString();
    }
}
