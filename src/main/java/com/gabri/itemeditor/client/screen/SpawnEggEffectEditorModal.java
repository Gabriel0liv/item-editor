package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.itemeditor.client.widget.EffectChoiceWidget;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import com.gabri.itemeditor.editor.ItemEditorCompat.SpawnEggEffectEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class SpawnEggEffectEditorModal extends BabelPanel {
    private final Consumer<SpawnEggEffectEntry> onSave;
    private final Runnable onCancel;
    private final int entryIndex;

    private final List<MobEffect> allEffects = new ArrayList<>();
    private final List<MobEffect> filteredEffects = new ArrayList<>();

    private final BabelTextField idField;
    private final BabelTextField durationField;
    private final BabelTextField amplifierField;
    private final BabelTextField searchField;
    private final BabelScrollPane selectorList;

    private final BabelButton ambientButton;
    private final BabelButton particlesButton;
    private final BabelButton iconButton;

    private boolean selectingEffect;
    private boolean ambient;
    private boolean showParticles;
    private boolean showIcon;
    private String effectQuery = "";

    public SpawnEggEffectEditorModal(Font font, SpawnEggEffectEntry existing,
                                     Consumer<SpawnEggEffectEntry> onSave, Runnable onCancel) {
        this.onSave = onSave == null ? entry -> {} : onSave;
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        this.entryIndex = existing != null ? existing.index() : -1;
        this.ambient = existing != null && existing.ambient();
        this.showParticles = existing == null || existing.showParticles();
        this.showIcon = existing == null || existing.showIcon();

        for (MobEffect effect : ForgeRegistries.MOB_EFFECTS.getValues()) {
            if (effect != null) {
                this.allEffects.add(effect);
            }
        }
        this.allEffects.sort(Comparator.comparing(SpawnEggEffectEditorModal::effectIdString,
                String.CASE_INSENSITIVE_ORDER));
        this.filteredEffects.addAll(this.allEffects);

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

        this.idField = new BabelTextField(existing != null ? existing.id() : "", value -> {});
        this.idField.maxLength(128).fillWidth();

        this.durationField = new BabelTextField(existing != null ? Integer.toString(existing.duration()) : "10", value -> {});
        this.durationField.maxLength(16).inputFilter(value -> value.isEmpty() || value.matches("\\d+")).fillWidth();

        this.amplifierField = new BabelTextField(existing != null ? Integer.toString(existing.amplifier()) : "0", value -> {});
        this.amplifierField.maxLength(8).inputFilter(value -> value.isEmpty() || value.matches("\\d+")).fillWidth();

        this.searchField = new BabelTextField("", value -> {
            this.effectQuery = value == null ? "" : value;
            filterEffects(this.effectQuery);
            rebuildSelectorList();
        });
        this.searchField.placeholder(Component.literal("Search effects"));
        this.searchField.maxLength(64).fillWidth();

        this.selectorList = new BabelScrollPane().fillWidth().height(selectorHeight);
        this.selectorList.showScrollbar(true);

        this.ambientButton = stateButton("Ambient", this.ambient, this::toggleAmbient);
        this.particlesButton = stateButton("Particles", this.showParticles, this::toggleParticles);
        this.iconButton = stateButton("Icon", this.showIcon, this::toggleIcon);

        rebuild();
    }

    private void rebuild() {
        clear();

        add(new BabelLabel(Component.literal(this.selectingEffect ? "Select Effect" : "Edit Mob Effect")));
        if (this.selectingEffect) {
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

        BabelRow effectRow = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        effectRow.add(this.idField.fillWidth());
        effectRow.add(new BabelButton(Component.literal("Select"), this::openSelector)
                .colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        add(BabelTemplates.settingsRow(Component.literal("Effect ID"), effectRow));
        add(BabelTemplates.settingsRow(Component.literal("Duration (s)"), this.durationField));
        add(BabelTemplates.settingsRow(Component.literal("Amplifier"), this.amplifierField));
        add(flagsGrid());
        add(actionsRow(true));
    }

    private BabelWidget flagsGrid() {
        BabelColumn column = new BabelColumn().fillWidth().gap(6);
        column.add(flagRow(this.ambientButton, this.particlesButton));
        column.add(flagRow(this.iconButton, new BabelSpacer(0, 20).fillWidth()));
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
        this.selectingEffect = true;
        this.effectQuery = "";
        this.searchField.setValueSilently("");
        filterEffects(this.effectQuery);
        rebuild();
    }

    private void closeSelector() {
        this.selectingEffect = false;
        rebuild();
    }

    private void filterEffects(String query) {
        this.filteredEffects.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (MobEffect effect : this.allEffects) {
            String id = effectIdString(effect).toLowerCase(Locale.ROOT);
            String description = effect.getDescriptionId() == null
                    ? ""
                    : effect.getDescriptionId().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || id.contains(normalized) || description.contains(normalized)) {
                this.filteredEffects.add(effect);
            }
        }
    }

    private void rebuildSelectorList() {
        this.selectorList.clear();
        if (this.filteredEffects.isEmpty()) {
            this.selectorList.add(BabelTemplates.emptyState(Component.literal("No effects"),
                    Component.literal("No matching mob effects were found.")));
            return;
        }

        String selected = ItemEditorCompat.normalizeId(this.idField.value());
        for (MobEffect effect : this.filteredEffects) {
            String effectId = effectIdString(effect);
            EffectChoiceWidget row = new EffectChoiceWidget(effect, effectId.equals(selected), () -> {
                this.idField.setValueSilently(effectId);
                this.selectingEffect = false;
                rebuild();
            }).fillWidth();
            this.selectorList.add(row);
        }
    }

    private BabelButton stateButton(String label, boolean selected, Runnable action) {
        BabelButton button = new BabelButton(Component.literal(label), action);
        updateFlagButton(button, selected);
        return button;
    }

    private void updateFlagButton(BabelButton button, boolean selected) {
        button.colors(selected ? 0xFF2F8F4E : 0xFF9E3C3C, selected ? 0xFF4FD67A : 0xFFC85A5A, BabelTheme.TEXT);
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

    private void save() {
        String id = ItemEditorCompat.normalizeId(this.idField.value());
        if (id.isEmpty()) {
            return;
        }
        int duration = parseInt(this.durationField.value(), 0);
        int amplifier = parseInt(this.amplifierField.value(), 0);
        this.onSave.accept(new SpawnEggEffectEntry(id, duration, amplifier, this.ambient, this.showParticles,
                this.showIcon, this.entryIndex));
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String effectIdString(MobEffect effect) {
        var id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return id == null ? "unknown" : id.toString();
    }
}
