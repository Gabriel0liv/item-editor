package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.itemeditor.client.widget.ItemStackPreviewWidget;
import com.gabri.itemeditor.client.screen.EnchantmentSelectionModal;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import com.gabri.itemeditor.editor.ItemEditorCompat.AttributeEntry;
import com.gabri.itemeditor.editor.ItemEditorCompat.EffectCategory;
import com.gabri.itemeditor.editor.ItemEditorCompat.EffectEntry;
import com.gabri.itemeditor.editor.ItemEditorCompat.EnchantmentEntry;
import com.gabri.itemeditor.editor.ItemEditorCompat.SpawnEggAttributeEntry;
import com.gabri.itemeditor.editor.ItemEditorCompat.SpawnEggEffectEntry;
import com.gabri.itemeditor.network.ItemEditorNetwork;
import com.gabri.itemeditor.network.SaveItemEditorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ItemEditorScreen extends BabelScreen {
    private enum Tab {
        GENERAL("General"),
        SPAWN_EGG("Spawn Egg"),
        ATTRIBUTES("Attributes"),
        ENCHANTMENTS("Enchantments"),
        EFFECTS("Effects"),
        HIDDEN_FLAGS("Hidden Flags");

        private final String label;

        Tab(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private enum SpawnEggTab {
        PROPERTIES("Properties"),
        EQUIPMENT("Equipment"),
        LOOT("Loot"),
        ATTRIBUTES("Attributes"),
        EFFECTS("Effects");

        private final String label;

        SpawnEggTab(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private enum HideFlagOption {
        ENCHANTMENTS("HideFlags", 1, "itemeditor.hide_flag.enchantments"),
        MODIFIERS("HideFlags", 2, "itemeditor.hide_flag.modifiers"),
        UNBREAKABLE("HideFlags", 4, "itemeditor.hide_flag.unbreakable"),
        CAN_DESTROY("HideFlags", 8, "itemeditor.hide_flag.can_destroy"),
        CAN_PLACE("HideFlags", 16, "itemeditor.hide_flag.can_place"),
        MISCELLANEOUS("HideFlags", 32, "itemeditor.hide_flag.miscellaneous"),
        DYE("HideFlags", 64, "itemeditor.hide_flag.dye"),
        UPGRADES("HideFlags", 128, "itemeditor.hide_flag.upgrades"),
        EFFECT_ON_USE("SF_HideEffectFlags", 1, "itemeditor.hide_flag.effect_on_use"),
        EFFECT_ON_HIT("SF_HideEffectFlags", 2, "itemeditor.hide_flag.effect_on_hit"),
        EFFECT_ON_HURT("SF_HideEffectFlags", 4, "itemeditor.hide_flag.effect_on_hurt"),
        EFFECT_ON_EQUIP("SF_HideEffectFlags", 8, "itemeditor.hide_flag.effect_on_equip");

        static final HideFlagOption[] ORDERED = {
                ENCHANTMENTS,
                MODIFIERS,
                UNBREAKABLE,
                CAN_DESTROY,
                CAN_PLACE,
                MISCELLANEOUS,
                DYE,
                UPGRADES,
                EFFECT_ON_USE,
                EFFECT_ON_HIT,
                EFFECT_ON_HURT,
                EFFECT_ON_EQUIP
        };

        private final String tagName;
        private final int value;
        private final String labelKey;

        HideFlagOption(String tagName, int value, String labelKey) {
            this.tagName = tagName;
            this.value = value;
            this.labelKey = labelKey;
        }

        String tagName() {
            return this.tagName;
        }

        int value() {
            return this.value;
        }

        String labelKey() {
            return this.labelKey;
        }
    }

    private final int containerId;
    private final int slotIndex;
    private ItemStack workingStack;
    private CompoundTag currentTag;
    private Tab currentTab = Tab.GENERAL;
    private EffectCategory currentEffectCategory = EffectCategory.ON_USE;
    private SpawnEggTab currentSpawnEggTab = SpawnEggTab.PROPERTIES;
    private String statusText = "itemeditor.ui.status.default";
    private static final int ROOT_GAP = 10;
    private static final int SIDEBAR_WIDTH = 172;
    private static final int PREVIEW_WIDGET_HEIGHT = 220;
    private static final int LIST_WIDGET_HEIGHT = 250;
    private static final String SF_HIDE_EFFECT_FLAGS_TAG = "SF_HideEffectFlags";
    private static final int ATTRIBUTE_ID_WEIGHT = 50;
    private static final int ATTRIBUTE_VALUE_WEIGHT = 15;
    private static final int ATTRIBUTE_SLOT_WEIGHT = 15;
    private static final int ATTRIBUTE_ACTIONS_WEIGHT = 20;
    private static final int ENCHANTMENT_NAME_WEIGHT = 56;
    private static final int ENCHANTMENT_LEVEL_WEIGHT = 20;
    private static final int ENCHANTMENT_ACTIONS_WEIGHT = 24;
    private static final int EFFECT_ID_WEIGHT = 37;
    private static final int EFFECT_DURATION_WEIGHT = 15;
    private static final int EFFECT_AMPLIFIER_WEIGHT = 15;
    private static final int EFFECT_ACTIONS_WEIGHT = 25;

    public ItemEditorScreen(ItemStack stack, int containerId, int slotIndex) {
        super(Component.translatable("itemeditor.ui.title"));
        this.containerId = containerId;
        this.slotIndex = slotIndex;
        this.workingStack = ItemEditorCompat.sanitizeStack(stack);
        this.currentTag = this.workingStack.hasTag() ? this.workingStack.getTag().copy() : new CompoundTag();
        if (isSpawnEggEditor()) {
            this.currentTab = Tab.SPAWN_EGG;
            this.statusText = "itemeditor.ui.status.spawn_egg";
        }
        syncWorkingStackTag();
    }

    @Override
    protected BabelWidget buildRoot(int width, int height) {
        BabelColumn shell = BabelTemplates.screenShell().gap(ROOT_GAP);
        shell.add(buildHeader());
        BabelRow body = new BabelRow().fillWidth().fillHeight().gap(ROOT_GAP);
        body.add(buildSidebar());
        body.add(buildContentPanel());
        shell.add(body);
        shell.add(buildFooter());
        return shell;
    }

    private BabelWidget buildHeader() {
        BabelRow row = new BabelRow().fillWidth().align(BabelAlign.CENTER);
        BabelLabel title = new BabelLabel(Component.translatable("itemeditor.ui.title"));
        title.style().textColor(BabelTheme.TEXT);
        row.add(title);
        return row;
    }

    private BabelWidget buildSidebar() {
        BabelPanel sidebar = new BabelPanel();
        sidebar.width(SIDEBAR_WIDTH);
        sidebar.fillHeight();
        sidebar.gap(6);
        sidebar.add(sidebarButton(Tab.GENERAL));
        if (isSpawnEggEditor()) {
            sidebar.add(sidebarButton(Tab.SPAWN_EGG));
        }
        sidebar.add(sidebarButton(Tab.ATTRIBUTES));
        sidebar.add(sidebarButton(Tab.ENCHANTMENTS));
        sidebar.add(sidebarButton(Tab.EFFECTS));
        sidebar.add(sidebarButton(Tab.HIDDEN_FLAGS));
        return sidebar;
    }

    private BabelButton sidebarButton(Tab tab) {
        boolean selected = this.currentTab == tab;
        BabelButton button = new BabelButton(
                Component.translatable("itemeditor.tab." + tab.name().toLowerCase(java.util.Locale.ROOT)),
                () -> selectCategory(tab));
        button.fillWidth();
        button.height(20);
        button.colors(selected ? BabelTheme.ACCENT_HOVER : BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        return button;
    }

    private void selectCategory(Tab tab) {
        if (tab == this.currentTab) {
            return;
        }

        this.currentTab = tab;
        if (tab == Tab.GENERAL) {
            this.statusText = "itemeditor.ui.status.general";
        } else if (tab == Tab.SPAWN_EGG) {
            this.statusText = "itemeditor.ui.status.spawn_egg";
            this.currentSpawnEggTab = SpawnEggTab.PROPERTIES;
        } else if (tab == Tab.ATTRIBUTES) {
            this.statusText = "itemeditor.ui.status.attributes";
        } else if (tab == Tab.ENCHANTMENTS) {
            this.statusText = "itemeditor.ui.status.enchantments";
        } else if (tab == Tab.EFFECTS) {
            this.statusText = "itemeditor.ui.status.effects";
            if (this.currentEffectCategory == EffectCategory.POTION) {
                this.currentEffectCategory = EffectCategory.ON_USE;
            }
        } else if (tab == Tab.HIDDEN_FLAGS) {
            this.statusText = "itemeditor.ui.status.hidden_flags";
        }
        this.init();
    }

    private BabelWidget buildContentPanel() {
        BabelPanel panel = new BabelPanel();
        panel.fillWidth();
        panel.fillHeight();
        panel.gap(8);

        BabelScrollPane body = new BabelScrollPane().fillWidth().fillHeight();
        body.showScrollbar(true);
        body.add(buildTabContent());
        panel.add(body);
        return panel;
    }

    private BabelWidget badgeLine(String label, String value) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        BabelBadge left = BabelTemplates
                .badge(isTranslationKey(label) ? Component.translatable(label) : Component.literal(label));
        BabelBadge right = BabelTemplates
                .badge(isTranslationKey(value) ? Component.translatable(value) : Component.literal(value));
        right.colors(BabelTheme.PANEL_ALT, BabelTheme.BORDER, BabelTheme.TEXT);
        row.add(left, right);
        return row;
    }

    private BabelWidget buildTabContent() {
        return switch (this.currentTab) {
            case GENERAL -> buildGeneralTab();
            case SPAWN_EGG -> buildSpawnEggTab();
            case ATTRIBUTES -> buildAttributesTab();
            case ENCHANTMENTS -> buildEnchantmentsTab();
            case EFFECTS -> buildEffectsTab();
            case HIDDEN_FLAGS -> buildHiddenFlagsTab();
        };
    }

    private BabelWidget buildGeneralTab() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        ItemStackPreviewWidget preview = new ItemStackPreviewWidget(() -> workingStack);
        preview.height(84);
        preview.fillWidth();
        column.add(preview);

        column.add(BabelTemplates.settingsRow(Component.translatable("itemeditor.ui.item_id"),
                fieldWithValue(getItemIdString(), this::setItemIdFromInput)));
        column.add(BabelTemplates.settingsRow(Component.translatable("itemeditor.ui.count"),
                fieldWithValue(Integer.toString(this.workingStack.getCount()), this::setCountFromInput).maxLength(4)
                        .inputFilter(value -> value.isEmpty() || value.matches("\\d+"))));
        column.add(BabelTemplates.settingsRow(Component.translatable("itemeditor.ui.damage"), fieldWithValue(
                this.workingStack.isDamageableItem() ? Integer.toString(this.workingStack.getDamageValue()) : "0",
                this::setDamageFromInput).maxLength(6).inputFilter(value -> value.isEmpty() || value.matches("\\d+"))));
        return column;
    }

    private BabelWidget buildSpawnEggTab() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow tabs = new BabelRow().fillWidth().gap(6);
        for (SpawnEggTab tab : SpawnEggTab.values()) {
            boolean selected = this.currentSpawnEggTab == tab;
            BabelButton button = new BabelButton(Component.literal(tab.label()), () -> {
                this.currentSpawnEggTab = tab;
                this.statusText = "Editing spawn egg " + tab.label().toLowerCase() + ".";
                this.init();
            }).fillWidth();
            button.colors(selected ? BabelTheme.ACCENT_HOVER : BabelTheme.PANEL_ALT, BabelTheme.ACCENT,
                    BabelTheme.TEXT);
            tabs.add(button);
        }
        column.add(tabs);

        column.add(switch (this.currentSpawnEggTab) {
            case PROPERTIES -> buildSpawnEggPropertiesTab();
            case EQUIPMENT -> buildSpawnEggEquipmentTab();
            case LOOT -> buildSpawnEggLootTab();
            case ATTRIBUTES -> buildSpawnEggAttributesTab();
            case EFFECTS -> buildSpawnEggEffectsTab();
        });
        return column;
    }

    private BabelWidget buildAttributesTab() {
        List<AttributeEntry> entries = ItemEditorCompat.readAttributes(this.currentTag);
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow actions = new BabelRow().fillWidth().justify(BabelJustify.END);
        actions.add(
                new BabelButton(Component.translatable("itemeditor.ui.add_attribute"), () -> openAttributeModal(null))
                        .colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        column.add(actions);
        column.add(attributeTableHeader());

        BabelScrollPane list = new BabelScrollPane().fillWidth().height(LIST_WIDGET_HEIGHT).padding(0, 0, 0, 10);
        list.showScrollbar(true);
        if (entries.isEmpty()) {
            list.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_attributes"),
                    Component.translatable("itemeditor.ui.no_attributes_desc")));
        } else {
            for (AttributeEntry entry : entries) {
                list.add(attributeRow(entry));
            }
        }
        column.add(list);
        return column;
    }

    private BabelWidget buildEnchantmentsTab() {
        List<EnchantmentEntry> entries = ItemEditorCompat.readEnchantments(this.workingStack, this.currentTag);
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow actions = new BabelRow().fillWidth().justify(BabelJustify.END);
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.add_enchantment"),
                () -> openEnchantmentSelector(null))
                .colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        column.add(actions);
        column.add(enchantmentTableHeader());

        BabelScrollPane list = new BabelScrollPane().fillWidth().height(LIST_WIDGET_HEIGHT).padding(0, 0, 0, 10);
        list.showScrollbar(true);
        if (entries.isEmpty()) {
            list.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_enchantments"),
                    Component.empty()));
        } else {
            for (EnchantmentEntry entry : entries) {
                list.add(enchantmentRow(entry));
            }
        }
        column.add(list);
        return column;
    }

    private BabelWidget buildEffectsTab() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow tabs = new BabelRow().fillWidth().gap(6);
        for (EffectCategory category : EffectCategory.values()) {
            if (category.isPotion()) {
                continue;
            }
            boolean selected = this.currentEffectCategory == category;
            BabelButton button = new BabelButton(Component.literal(category.label()), () -> {
                this.currentEffectCategory = category;
                this.init();
            }).fillWidth();
            button.colors(selected ? BabelTheme.ACCENT_HOVER : BabelTheme.PANEL_ALT, BabelTheme.ACCENT,
                    BabelTheme.TEXT);
            tabs.add(button);
        }
        column.add(tabs);

        EffectCategory storageCategory = effectStorageCategory();
        List<EffectEntry> entries = ItemEditorCompat.readEffects(this.currentTag, storageCategory);
        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.add_effect"), () -> openEffectModal(null))
                .colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        if (storageCategory.isPotion()) {
            actions.add(potionColorButton());
        }
        column.add(actions);
        column.add(effectTableHeader());

        BabelScrollPane list = new BabelScrollPane().fillWidth().height(LIST_WIDGET_HEIGHT);
        list.showScrollbar(true);
        if (entries.isEmpty()) {
            list.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_effects"),
                    Component.translatable("itemeditor.ui.no_effects_desc")));
        } else {
            for (EffectEntry entry : entries) {
                list.add(effectRow(entry));
            }
        }
        column.add(list);
        return column;
    }

    private BabelWidget potionColorButton() {
        int currentColor = ItemEditorCompat.potionColor(this.currentTag);
        String label = currentColor >= 0
                ? Component.translatable("itemeditor.ui.color_with_value",
                        String.format(java.util.Locale.ROOT, "#%06X", currentColor & 0xFFFFFF)).getString()
                : Component.translatable("itemeditor.ui.pick_color").getString();
        BabelButton button = new BabelButton(Component.literal(label), this::openPotionColorModal);
        button.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        return button;
    }

    private BabelWidget buildHiddenFlagsTab() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelColumn toggles = new BabelColumn().fillWidth().gap(6);
        for (int i = 0; i < HideFlagOption.ORDERED.length; i += 2) {
            HideFlagOption left = HideFlagOption.ORDERED[i];
            HideFlagOption right = i + 1 < HideFlagOption.ORDERED.length ? HideFlagOption.ORDERED[i + 1] : null;
            toggles.add(hiddenFlagRow(left, right));
        }
        column.add(toggles);

        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.clear_flags"), this::clearHiddenFlags)
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        column.add(actions);
        return column;
    }

    private void openPotionColorModal() {
        Integer currentColor = ItemEditorCompat.potionColor(this.currentTag);
        this.openModal(new PotionColorModal(this.font, currentColor, color -> {
            ItemEditorCompat.setPotionColor(this.currentTag, color);
            syncWorkingStackTag();
            this.closeModal();
            this.init();
        }, () -> {
            this.closeModal();
        }));
    }

    private BabelWidget buildSpawnEggPropertiesTab() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelColumn toggles = new BabelColumn().fillWidth().gap(6);
        toggles.add(spawnEggTogglePair(
                spawnEggToggleOption("Is Baby", null, () -> {
                    toggleSpawnEggBaby();
                    this.init();
                }),
                spawnEggToggleOption("No AI", "NoAI", null)));
        toggles.add(spawnEggTogglePair(
                spawnEggToggleOption("Invulnerable", "Invulnerable", null),
                spawnEggToggleOption("PersistenceRequired", "PersistenceRequired", null)));
        toggles.add(spawnEggTogglePair(
                spawnEggToggleOption("Glowing", "Glowing", null),
                spawnEggToggleOption("Silent", "Silent", null)));
        toggles.add(spawnEggTogglePair(
                spawnEggToggleOption("NoGravity", "NoGravity", null),
                null));
        column.add(BabelTemplates.settingsGroup(Component.translatable("itemeditor.ui.flags"), toggles));
        return column;
    }

    private BabelWidget buildSpawnEggEquipmentTab() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelLabel hint = new BabelLabel(Component.translatable("itemeditor.ui.equipment_desc"));
        hint.wrap(true);
        hint.style().textColor(BabelTheme.TEXT_MUTED);
        column.add(hint);
        CompoundTag entityTag = spawnEggEntityTag();
        for (SpawnEggEquipmentSlot slot : SpawnEggEquipmentSlot.ORDERED) {
            column.add(spawnEggEquipmentRow(entityTag, slot));
        }
        return column;
    }

    private BabelWidget buildSpawnEggLootTab() {
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow lootRow = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        BabelTextField lootField = new BabelTextField(spawnEggLootTableValue(),
                value -> setSpawnEggLootTable(value == null ? "" : value.trim()));
        lootField.maxLength(256).fillWidth();
        lootRow.add(lootField);
        lootRow.add(
                new BabelButton(Component.translatable("itemeditor.ui.select_loot_table"), this::openLootTableSelector)
                        .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        column.add(BabelTemplates.settingsRow(Component.translatable("itemeditor.ui.death_loot_table"), lootRow));

        BabelRow actions = new BabelRow().fillWidth().gap(6);
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.use_default"), () -> {
            setSpawnEggLootTable(ItemEditorCompat.spawnEggDefaultLootTable(this.workingStack, this.currentTag));
            this.init();
        }).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.clear"), () -> {
            setSpawnEggLootTable("minecraft:empty");
            this.init();
        }).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        column.add(actions);
        return column;
    }

    private BabelWidget buildSpawnEggAttributesTab() {
        CompoundTag entityTag = spawnEggEntityTag();
        List<SpawnEggAttributeEntry> entries = ItemEditorCompat.readSpawnEggAttributes(entityTag);
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow actions = new BabelRow().fillWidth().justify(BabelJustify.END);
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.add_attribute"),
                () -> openSpawnEggAttributeModal(null))
                .colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        column.add(actions);

        BabelScrollPane list = new BabelScrollPane().fillWidth().height(LIST_WIDGET_HEIGHT);
        list.showScrollbar(true);
        if (entries.isEmpty()) {
            list.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_attributes"),
                    Component.translatable("itemeditor.ui.spawn_attributes_desc")));
        } else {
            for (SpawnEggAttributeEntry entry : entries) {
                list.add(spawnEggAttributeRow(entry));
            }
        }
        column.add(list);
        return column;
    }

    private BabelWidget buildSpawnEggEffectsTab() {
        CompoundTag entityTag = spawnEggEntityTag();
        List<SpawnEggEffectEntry> entries = ItemEditorCompat.readSpawnEggEffects(entityTag);
        BabelColumn column = new BabelColumn().fillWidth().gap(8);
        BabelRow actions = new BabelRow().fillWidth().justify(BabelJustify.END);
        actions.add(
                new BabelButton(Component.translatable("itemeditor.ui.add_effect"), () -> openSpawnEggEffectModal(null))
                        .colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        column.add(actions);

        BabelScrollPane list = new BabelScrollPane().fillWidth().height(LIST_WIDGET_HEIGHT);
        list.showScrollbar(true);
        if (entries.isEmpty()) {
            list.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_effects"),
                    Component.translatable("itemeditor.ui.spawn_effects_desc")));
        } else {
            for (SpawnEggEffectEntry entry : entries) {
                list.add(spawnEggEffectRow(entry));
            }
        }
        column.add(list);
        return column;
    }

    private BabelWidget spawnEggTogglePair(BabelWidget left, BabelWidget right) {
        BabelRow row = new BabelRow().fillWidth().gap(6);
        row.add(left);
        if (right != null) {
            row.add(right);
        } else {
            row.add(BabelUi.spacer(0, 1).flexGrow(1));
        }
        return row;
    }

    private BabelWidget spawnEggToggleOption(String label, String key, Runnable customToggle) {
        boolean enabled = key == null
                ? ItemEditorCompat.isSpawnEggBaby(spawnEggEntityTag())
                : ItemEditorCompat.isSpawnEggFlagEnabled(spawnEggEntityTag(), key);
        Runnable onToggle = customToggle != null ? customToggle : () -> {
            toggleSpawnEggFlag(key);
            this.init();
        };
        BabelButton button = new BabelButton(Component.literal(label), onToggle);
        button.fillWidth();
        button.height(22);
        button.colors(enabled ? BabelTheme.SUCCESS : BabelTheme.DANGER, enabled ? 0xFF87D989 : 0xFFFF8080,
                BabelTheme.TEXT);
        return button;
    }

    private BabelWidget spawnEggEquipmentRow(CompoundTag entityTag, SpawnEggEquipmentSlot slot) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        BabelLabel label = new BabelLabel(Component.literal(slot.label()));
        label.style().width(98);
        row.add(label);

        ItemStack stack = spawnEggEquipmentStack(entityTag, slot);
        String summary = stack.isEmpty() ? "(empty)" : stack.getHoverName().getString();
        BabelLabel itemLabel = new BabelLabel(Component.literal(summary));
        itemLabel.style().fillWidth(1);
        itemLabel.style().minWidth(180);
        itemLabel.wrap(true);
        row.add(itemLabel);

        BabelTextField chanceField = new BabelTextField(formatDropChance(spawnEggEquipmentChance(entityTag, slot)),
                value -> setSpawnEggEquipmentChance(slot, value));
        chanceField.maxLength(16).inputFilter(value -> value.isEmpty() || value.matches("[-0-9.]+")).width(70);
        row.add(chanceField);

        row.add(new BabelButton(Component.translatable("itemeditor.ui.select"),
                () -> this.openModal(new ItemStackSelectorModal(this.font, picked -> {
                    setSpawnEggEquipmentStack(slot, picked);
                    this.closeModal();
                    this.init();
                }, this::closeModal))).colors(BabelTheme.ACCENT, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));

        row.add(new BabelButton(Component.translatable("itemeditor.ui.clear"), () -> {
            clearSpawnEggEquipmentStack(slot);
            this.init();
        }).colors(BabelTheme.DANGER, 0xFFFF8080, BabelTheme.TEXT));
        return row;
    }

    private BabelWidget spawnEggAttributeRow(SpawnEggAttributeEntry entry) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        BabelLabel name = new BabelLabel(Component.literal(attributeDisplayName(entry.attributeName())));
        name.style().flexGrow(1);
        name.wrap(true);
        row.add(name);

        BabelLabel base = new BabelLabel(Component.literal(formatAttributeValue(entry.base())));
        base.style().width(92);
        row.add(base);

        row.add(new BabelButton(Component.translatable("itemeditor.ui.edit"), () -> openSpawnEggAttributeModal(entry))
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        row.add(new BabelButton(Component.translatable("itemeditor.ui.remove"), () -> {
            removeSpawnEggAttribute(entry.index());
            this.init();
        }).colors(BabelTheme.DANGER, 0xFFFF8080, BabelTheme.TEXT));
        return row;
    }

    private BabelWidget spawnEggEffectRow(SpawnEggEffectEntry entry) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        row.add(new EffectIconWidget(entry.id()));

        BabelLabel name = new BabelLabel(Component.literal(effectName(entry.id())));
        name.style().flexGrow(1);
        name.wrap(true);
        row.add(name);

        BabelLabel duration = new BabelLabel(Component.literal(formatDuration(entry.duration())));
        duration.style().width(76);
        row.add(duration);

        BabelLabel amplifier = new BabelLabel(Component.literal("x" + Math.max(0, entry.amplifier())));
        amplifier.style().width(56);
        row.add(amplifier);

        row.add(new BabelButton(Component.translatable("itemeditor.ui.edit"), () -> openSpawnEggEffectModal(entry))
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        row.add(new BabelButton(Component.translatable("itemeditor.ui.remove"), () -> {
            removeSpawnEggEffect(entry.index());
            this.init();
        }).colors(BabelTheme.DANGER, 0xFFFF8080, BabelTheme.TEXT));
        return row;
    }

    private BabelWidget buildFooter() {
        BabelRow row = new BabelRow().fillWidth().gap(8).justify(BabelJustify.END);
        row.add(
                new BabelButton(Component.literal("Save"), this::save).colors(BabelTheme.SUCCESS, 0xFF87D989,
                        BabelTheme.TEXT),
                new BabelButton(Component.literal("Cancel"), () -> this.minecraft.setScreen(null))
                        .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        return row;
    }

    private BabelWidget sectionHeader(String title, int count, String actionLabel, Runnable action) {
        BabelRow row = new BabelRow().fillWidth().gap(8).align(BabelAlign.CENTER);
        BabelLabel label = new BabelLabel(Component.literal(title + " (" + count + ")"));
        label.style().textColor(BabelTheme.TEXT);
        label.style().flexGrow(1);
        row.add(label);
        row.add(new BabelButton(Component.literal(actionLabel), action).colors(BabelTheme.ACCENT,
                BabelTheme.ACCENT_HOVER, BabelTheme.TEXT));
        return row;
    }

    private BabelWidget attributeRow(AttributeEntry entry) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER).padding(6, 10, 6, 10);
        BabelLabel attribute = new BabelLabel(Component.literal(entry.attributeName()));
        attribute.style().fillWidth(ATTRIBUTE_ID_WEIGHT);
        attribute.style().alignSelf(BabelAlign.START);
        attribute.wrap(true);
        row.add(attribute);

        BabelLabel value = new BabelLabel(Component.literal(formatAttributeValue(entry.amount())));
        value.style().fillWidth(ATTRIBUTE_VALUE_WEIGHT);
        row.add(value);

        BabelLabel slot = new BabelLabel(Component.literal(slotDisplayName(entry.slot())));
        slot.style().fillWidth(ATTRIBUTE_SLOT_WEIGHT);
        row.add(slot);

        BabelRow buttons = new BabelRow().fillWidth(ATTRIBUTE_ACTIONS_WEIGHT).align(BabelAlign.CENTER);
        BabelSpacer leftSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelSpacer rightSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelRow buttonCluster = new BabelRow().gap(4).autoWidth().align(BabelAlign.CENTER);
        BabelButton editButton = new BabelButton(Component.literal("Edit"), () -> openAttributeModal(entry));
        editButton.width(36);
        editButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        BabelButton deleteButton = new BabelButton(Component.literal("X"), () -> {
            removeAttribute(entry);
            this.init();
        });
        deleteButton.width(20);
        deleteButton.colors(BabelTheme.DANGER, 0xFFFF8080, BabelTheme.TEXT);
        buttonCluster.add(editButton, deleteButton);
        buttons.add(leftSpacer, buttonCluster, rightSpacer);
        row.add(buttons);
        return row;
    }

    private BabelWidget attributeTableHeader() {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER).padding(8, 10, 8, 10);
        row.background(BabelTheme.PANEL);
        row.border(BabelTheme.BORDER, 1);

        BabelLabel attribute = new BabelLabel(Component.literal("Attribute ID"));
        attribute.style().fillWidth(ATTRIBUTE_ID_WEIGHT);
        attribute.style().alignSelf(BabelAlign.START);
        attribute.style().padding(0, 0, 0, 0);
        attribute.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(attribute);

        BabelLabel value = new BabelLabel(Component.literal("Value"));
        value.style().fillWidth(ATTRIBUTE_VALUE_WEIGHT);
        value.style().padding(0, 0, 0, 0);
        value.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(value);

        BabelLabel slot = new BabelLabel(Component.literal("Slot"));
        slot.style().fillWidth(ATTRIBUTE_SLOT_WEIGHT);
        slot.style().padding(0, 0, 0, 0);
        slot.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(slot);

        BabelRow actions = new BabelRow().fillWidth(ATTRIBUTE_ACTIONS_WEIGHT).align(BabelAlign.CENTER);
        BabelSpacer leftSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelSpacer rightSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelLabel actionsLabel = new BabelLabel(Component.literal("Actions"));
        actionsLabel.style().textColor(BabelTheme.TEXT_MUTED);
        actionsLabel.style().padding(0, 0, 0, 0);
        actions.add(leftSpacer, actionsLabel, rightSpacer);
        row.add(actions);
        return row;
    }

    private BabelWidget enchantmentRow(EnchantmentEntry entry) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER).padding(6, 10, 6, 10);

        BabelLabel enchantment = new BabelLabel(Component.literal(enchantmentDisplayName(entry.id())));
        enchantment.style().fillWidth(ENCHANTMENT_NAME_WEIGHT);
        enchantment.style().alignSelf(BabelAlign.START);
        enchantment.wrap(true);
        row.add(enchantment);

        BabelRow levelCell = new BabelRow().fillWidth(ENCHANTMENT_LEVEL_WEIGHT).gap(4).align(BabelAlign.CENTER);
        BabelButton decrement = new BabelButton(Component.literal("-"), () -> {
            setEnchantmentLevel(entry, Math.max(1, entry.level() - 1));
            this.init();
        });
        decrement.width(18);
        decrement.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        BabelTextField levelField = new BabelTextField(Integer.toString(Math.max(1, entry.level())),
                value -> updateEnchantmentLevel(entry, value));
        levelField.maxLength(4).inputFilter(value -> value.isEmpty() || value.matches("\\d+")).width(40);
        BabelButton increment = new BabelButton(Component.literal("+"), () -> {
            setEnchantmentLevel(entry, entry.level() + 1);
            this.init();
        });
        increment.width(18);
        increment.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        levelCell.add(decrement, levelField, increment);
        row.add(levelCell);

        BabelRow buttons = new BabelRow().fillWidth(ENCHANTMENT_ACTIONS_WEIGHT).align(BabelAlign.CENTER);
        BabelSpacer leftSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelSpacer rightSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelRow buttonCluster = new BabelRow().gap(4).autoWidth().align(BabelAlign.CENTER);
        BabelButton editButton = new BabelButton(Component.translatable("itemeditor.ui.edit"),
                () -> openEnchantmentSelector(entry));
        editButton.width(36);
        editButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        BabelButton deleteButton = new BabelButton(Component.translatable("itemeditor.ui.remove"), () -> {
            removeEnchantment(entry);
            this.init();
        });
        deleteButton.width(46);
        deleteButton.colors(BabelTheme.DANGER, 0xFFFF8080, BabelTheme.TEXT);
        buttonCluster.add(editButton, deleteButton);
        buttons.add(leftSpacer, buttonCluster, rightSpacer);
        row.add(buttons);
        return row;
    }

    private BabelWidget enchantmentTableHeader() {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER).padding(8, 10, 8, 10);
        row.background(BabelTheme.PANEL);
        row.border(BabelTheme.BORDER, 1);

        BabelLabel enchantment = new BabelLabel(Component.translatable("itemeditor.ui.enchantment"));
        enchantment.style().fillWidth(ENCHANTMENT_NAME_WEIGHT);
        enchantment.style().alignSelf(BabelAlign.START);
        enchantment.style().padding(0, 0, 0, 0);
        enchantment.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(enchantment);

        BabelLabel level = new BabelLabel(Component.translatable("itemeditor.ui.level"));
        level.style().fillWidth(ENCHANTMENT_LEVEL_WEIGHT);
        level.style().padding(0, 0, 0, 0);
        level.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(level);

        BabelRow actions = new BabelRow().fillWidth(ENCHANTMENT_ACTIONS_WEIGHT).align(BabelAlign.CENTER);
        BabelSpacer leftSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelSpacer rightSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelLabel actionsLabel = new BabelLabel(Component.translatable("itemeditor.ui.actions"));
        actionsLabel.style().padding(0, 0, 0, 0);
        actionsLabel.style().textColor(BabelTheme.TEXT_MUTED);
        actions.add(leftSpacer, actionsLabel, rightSpacer);
        row.add(actions);
        return row;
    }

    private BabelWidget effectRow(EffectEntry entry) {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER).padding(6, 10, 6, 10);
        row.add(new EffectIconWidget(entry.id()));

        BabelLabel effect = new BabelLabel(Component.literal(effectName(entry.id())));
        effect.style().fillWidth(EFFECT_ID_WEIGHT);
        effect.style().alignSelf(BabelAlign.START);
        effect.wrap(true);
        row.add(effect);

        BabelLabel duration = new BabelLabel(Component.literal(formatDuration(entry.duration())));
        duration.style().fillWidth(EFFECT_DURATION_WEIGHT);
        row.add(duration);

        BabelLabel amplifier = new BabelLabel(Component.literal("x" + Math.max(0, entry.amplifier())));
        amplifier.style().fillWidth(EFFECT_AMPLIFIER_WEIGHT);
        row.add(amplifier);

        BabelRow buttons = new BabelRow().fillWidth(EFFECT_ACTIONS_WEIGHT).align(BabelAlign.CENTER);
        if (isSyntheticPotionEffect(entry)) {
            BabelSpacer leftSpacer = BabelUi.spacer(0, 1).flexGrow(1);
            BabelSpacer rightSpacer = BabelUi.spacer(0, 1).flexGrow(1);
            BabelBadge badge = BabelTemplates.badge(Component.literal("Base"));
            badge.colors(BabelTheme.PANEL_ALT, BabelTheme.BORDER, BabelTheme.TEXT_MUTED);
            buttons.add(leftSpacer, badge, rightSpacer);
        } else {
            BabelSpacer leftSpacer = BabelUi.spacer(0, 1).flexGrow(1);
            BabelSpacer rightSpacer = BabelUi.spacer(0, 1).flexGrow(1);
            BabelRow buttonCluster = new BabelRow().gap(4).autoWidth().align(BabelAlign.CENTER);
            BabelButton editButton = new BabelButton(Component.literal("Edit"), () -> openEffectModal(entry));
            editButton.width(36);
            editButton.colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
            BabelButton deleteButton = new BabelButton(Component.literal("X"), () -> {
                removeEffect(entry);
                this.init();
            });
            deleteButton.width(20);
            deleteButton.colors(BabelTheme.DANGER, 0xFFFF8080, BabelTheme.TEXT);
            buttonCluster.add(editButton, deleteButton);
            buttons.add(leftSpacer, buttonCluster, rightSpacer);
        }
        row.add(buttons);
        return row;
    }

    private BabelWidget effectTableHeader() {
        BabelRow row = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER).padding(8, 10, 8, 10);
        row.background(BabelTheme.PANEL);
        row.border(BabelTheme.BORDER, 1);

        BabelLabel icon = new BabelLabel(Component.literal("Icon"));
        icon.style().fillWidth(18);
        icon.style().padding(0, 0, 0, 0);
        icon.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(icon);

        BabelLabel effect = new BabelLabel(Component.literal("Effect ID"));
        effect.style().fillWidth(EFFECT_ID_WEIGHT);
        effect.style().alignSelf(BabelAlign.START);
        effect.style().padding(0, 0, 0, 0);
        effect.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(effect);

        BabelLabel duration = new BabelLabel(Component.literal("Duration"));
        duration.style().fillWidth(EFFECT_DURATION_WEIGHT);
        duration.style().padding(0, 0, 0, 0);
        duration.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(duration);

        BabelLabel amplifier = new BabelLabel(Component.literal("Amplifier"));
        amplifier.style().fillWidth(EFFECT_AMPLIFIER_WEIGHT);
        amplifier.style().padding(0, 0, 0, 0);
        amplifier.style().textColor(BabelTheme.TEXT_MUTED);
        row.add(amplifier);

        BabelRow actions = new BabelRow().fillWidth(EFFECT_ACTIONS_WEIGHT).align(BabelAlign.CENTER);
        BabelSpacer leftSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelSpacer rightSpacer = BabelUi.spacer(0, 1).flexGrow(1);
        BabelLabel actionsLabel = new BabelLabel(Component.literal("Actions"));
        actionsLabel.style().padding(0, 0, 0, 0);
        actionsLabel.style().textColor(BabelTheme.TEXT_MUTED);
        actions.add(leftSpacer, actionsLabel, rightSpacer);
        row.add(actions);
        return row;
    }

    private static String formatAttributeValue(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String slotDisplayName(String slot) {
        if (slot == null || slot.isBlank()) {
            return "any";
        }
        return slot;
    }

    private static boolean isSyntheticPotionEffect(EffectEntry entry) {
        return entry != null && entry.index() < 0;
    }

    private EffectCategory effectStorageCategory() {
        if (this.currentEffectCategory == EffectCategory.ON_USE && isPotionLikeOnUseItem()) {
            return EffectCategory.POTION;
        }
        return this.currentEffectCategory;
    }

    private boolean isPotionLikeOnUseItem() {
        if (this.workingStack == null || this.workingStack.isEmpty()) {
            return false;
        }
        return this.workingStack.is(Items.POTION)
                || this.workingStack.is(Items.SPLASH_POTION)
                || this.workingStack.is(Items.LINGERING_POTION)
                || this.currentTag.contains(ItemEditorCompat.POTION_EFFECTS_TAG, Tag.TAG_LIST)
                || this.currentTag.contains(ItemEditorCompat.POTION_BASE_TAG, Tag.TAG_STRING);
    }

    private static String formatDuration(int duration) {
        return duration + "s";
    }

    private static String formatChance(double chance) {
        return String.format(java.util.Locale.ROOT, "%.0f%%", Math.max(0.0D, chance) * 100.0D);
    }

    private static boolean isTranslationKey(String value) {
        return value != null && value.startsWith("itemeditor.");
    }

    private static String effectName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(ItemEditorCompat.normalizeId(id));
        if (key == null) {
            return id == null ? "unknown" : id;
        }
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(key);
        if (effect == null) {
            return id == null || id.isBlank() ? "unknown" : id;
        }
        Component translated = Component.translatable(effect.getDescriptionId());
        String text = translated.getString();
        return text == null || text.isBlank() ? key.toString() : text;
    }

    private static String attributeDisplayName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(ItemEditorCompat.normalizeId(id));
        if (key == null) {
            return id == null ? "unknown" : id;
        }
        var attribute = ForgeRegistries.ATTRIBUTES.getValue(key);
        if (attribute == null) {
            return id == null || id.isBlank() ? "unknown" : id;
        }
        Component translated = Component.translatable(attribute.getDescriptionId());
        String text = translated.getString();
        return text == null || text.isBlank() ? key.toString() : text;
    }

    private static final class EffectIconWidget extends BabelWidget {
        private ResourceLocation effectId;

        EffectIconWidget(String effectId) {
            this.effectId = ResourceLocation.tryParse(ItemEditorCompat.normalizeId(effectId));
            style().width(18);
            style().height(18);
        }

        @Override
        public BabelSize measure(net.minecraft.client.gui.Font font, int availableWidth, int availableHeight) {
            return resolveOuterSize(18, 18, availableWidth, availableHeight);
        }

        @Override
        public void render(GuiGraphics graphics, net.minecraft.client.gui.Font font, int mouseX, int mouseY,
                float partialTick) {
            drawBackground(graphics);
            if (this.effectId == null) {
                return;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(this.effectId);
            if (effect == null) {
                return;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
            if (sprite != null) {
                graphics.blit(bounds().x() + 1, bounds().y() + 1, 0, 16, 16, sprite);
            }
        }
    }

    private BabelTextField fieldWithValue(String value, java.util.function.Consumer<String> onChange) {
        return new BabelTextField(value, onChange).maxLength(128).fillWidth();
    }

    private String getItemIdString() {
        return ItemEditorCompat.itemId(this.workingStack).toString();
    }

    private void setItemIdFromInput(String raw) {
        String normalized = ItemEditorCompat.normalizeId(raw);
        if (normalized.isEmpty()) {
            return;
        }
        if (normalized.equals(getItemIdString())) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        if (id == null || ForgeRegistries.ITEMS.getValue(id) == null) {
            return;
        }
        ItemStack next = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        next.setCount(Math.max(1, Math.min(this.workingStack.getCount(), next.getMaxStackSize())));
        next.setTag(this.currentTag.isEmpty() ? null : this.currentTag.copy());
        if (next.isDamageableItem()) {
            next.setDamageValue(Math.max(0, Math.min(next.getMaxDamage(), this.workingStack.getDamageValue())));
        }
        this.workingStack = ItemEditorCompat.sanitizeStack(next);
        this.currentTag = this.workingStack.hasTag() ? this.workingStack.getTag().copy() : new CompoundTag();
        if (isSpawnEggEditor()) {
            this.currentTab = Tab.SPAWN_EGG;
            this.currentSpawnEggTab = SpawnEggTab.PROPERTIES;
        } else if (this.currentTab == Tab.SPAWN_EGG) {
            this.currentTab = Tab.GENERAL;
        }
        syncWorkingStackTag();
        this.init();
    }

    private void setCountFromInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            int count = Integer.parseInt(raw.trim());
            this.workingStack.setCount(Mth.clamp(count, 1, Math.max(1, this.workingStack.getMaxStackSize())));
        } catch (Exception ignored) {
        }
    }

    private void setDamageFromInput(String raw) {
        if (!this.workingStack.isDamageableItem() || raw == null || raw.isBlank()) {
            return;
        }
        try {
            int damage = Integer.parseInt(raw.trim());
            this.workingStack.setDamageValue(Mth.clamp(damage, 0, Math.max(0, this.workingStack.getMaxDamage())));
        } catch (Exception ignored) {
        }
    }

    private void toggleUnbreakable() {
        if (this.currentTag.getBoolean("Unbreakable")) {
            this.currentTag.remove("Unbreakable");
        } else {
            this.currentTag.putBoolean("Unbreakable", true);
        }
        syncWorkingStackTag();
        this.init();
    }

    private void openAttributeModal(AttributeEntry entry) {
        this.openModal(new AttributeEditorModal(this.font, entry, result -> {
            List<AttributeEntry> list = new ArrayList<>(ItemEditorCompat.readAttributes(this.currentTag));
            if (entry != null) {
                list.removeIf(candidate -> candidate.curio() == entry.curio() && candidate.index() == entry.index());
            }
            list.add(result);
            ItemEditorCompat.writeAttributes(this.currentTag, list);
            syncWorkingStackTag();
            this.closeModal();
            this.init();
        }, this::closeModal));
    }

    private void openEnchantmentSelector(EnchantmentEntry entry) {
        String selected = entry != null ? entry.id() : "";
        this.openModal(new EnchantmentSelectionModal(this.font, selected, chosen -> {
            List<EnchantmentEntry> list = new ArrayList<>(
                    ItemEditorCompat.readEnchantments(this.workingStack, this.currentTag));
            if (entry != null) {
                list.removeIf(candidate -> candidate.index() == entry.index());
            }
            int level = entry != null ? Math.max(1, entry.level()) : 1;
            int nextIndex = entry != null ? entry.index()
                    : list.stream().mapToInt(EnchantmentEntry::index).max().orElse(-1) + 1;
            list.add(new EnchantmentEntry(chosen, level, nextIndex));
            ItemEditorCompat.writeEnchantments(this.workingStack, this.currentTag, list);
            syncWorkingStackTag();
            this.closeModal();
            this.init();
        }, this::closeModal));
    }

    private void openLootTableSelector() {
        String selected = spawnEggLootTableValue();
        this.openModal(new LootTableSelectionModal(this.font, selected, chosen -> {
            setSpawnEggLootTable(chosen);
            this.closeModal();
            this.init();
        }, this::closeModal));
    }

    private void openEffectModal(EffectEntry entry) {
        EffectCategory logicalCategory = this.currentEffectCategory;
        EffectCategory storageCategory = effectStorageCategory();
        if (entry != null && storageCategory.isPotion() && isSyntheticPotionEffect(entry)) {
            return;
        }
        EffectEntry seed = entry;
        if (seed != null && storageCategory.isPotion() && seed.category() != EffectCategory.POTION) {
            seed = new EffectEntry(EffectCategory.POTION, seed.id(), seed.duration(), seed.amplifier(), seed.chance(),
                    false, "", seed.ambient(), seed.showParticles(), seed.showIcon(), seed.color(), seed.index());
        }
        this.openModal(new EffectEditorModal(this.font, logicalCategory, storageCategory, seed, result -> {
            List<EffectEntry> list = new ArrayList<>(ItemEditorCompat.readEffects(this.currentTag, storageCategory));
            if (entry != null) {
                list.removeIf(candidate -> candidate.index() == entry.index());
            }
            EffectEntry saved = result;
            if (storageCategory.isPotion() && result.index() < 0) {
                int nextIndex = list.stream()
                        .mapToInt(EffectEntry::index)
                        .filter(value -> value >= 0)
                        .max()
                        .orElse(-1) + 1;
                saved = new EffectEntry(
                        result.category(),
                        result.id(),
                        result.duration(),
                        result.amplifier(),
                        result.chance(),
                        result.self(),
                        result.slot(),
                        result.ambient(),
                        result.showParticles(),
                        result.showIcon(),
                        result.color(),
                        nextIndex);
            }
            list.add(saved);
            ItemEditorCompat.writeEffects(this.currentTag, storageCategory, list);
            syncWorkingStackTag();
            this.closeModal();
            this.init();
        }, this::closeModal));
    }

    private void updateEnchantmentLevel(EnchantmentEntry entry, String raw) {
        if (entry == null || raw == null || raw.isBlank()) {
            return;
        }
        try {
            int level = Math.max(1, Integer.parseInt(raw.trim()));
            setEnchantmentLevel(entry, level);
            syncWorkingStackTag();
        } catch (Exception ignored) {
        }
    }

    private void setEnchantmentLevel(EnchantmentEntry entry, int level) {
        List<EnchantmentEntry> list = new ArrayList<>(
                ItemEditorCompat.readEnchantments(this.workingStack, this.currentTag));
        for (int i = 0; i < list.size(); i++) {
            EnchantmentEntry candidate = list.get(i);
            if (candidate.index() == entry.index()) {
                list.set(i, new EnchantmentEntry(candidate.id(), Math.max(1, level), candidate.index()));
                break;
            }
        }
        ItemEditorCompat.writeEnchantments(this.workingStack, this.currentTag, list);
        syncWorkingStackTag();
    }

    private void removeEnchantment(EnchantmentEntry entry) {
        List<EnchantmentEntry> list = new ArrayList<>(
                ItemEditorCompat.readEnchantments(this.workingStack, this.currentTag));
        list.removeIf(candidate -> candidate.index() == entry.index());
        ItemEditorCompat.writeEnchantments(this.workingStack, this.currentTag, list);
        syncWorkingStackTag();
    }

    private void openSpawnEggAttributeModal(SpawnEggAttributeEntry entry) {
        this.openModal(new SpawnEggAttributeEditorModal(this.font, entry, result -> {
            List<SpawnEggAttributeEntry> list = new ArrayList<>(
                    ItemEditorCompat.readSpawnEggAttributes(spawnEggEntityTag()));
            if (entry != null) {
                list.removeIf(candidate -> candidate.index() == entry.index());
            }
            list.add(result);
            writeSpawnEggAttributes(list);
            this.closeModal();
            this.init();
        }, this::closeModal));
    }

    private void openSpawnEggEffectModal(SpawnEggEffectEntry entry) {
        this.openModal(new SpawnEggEffectEditorModal(this.font, entry, result -> {
            List<SpawnEggEffectEntry> list = new ArrayList<>(ItemEditorCompat.readSpawnEggEffects(spawnEggEntityTag()));
            if (entry != null) {
                list.removeIf(candidate -> candidate.index() == entry.index());
            }
            list.add(result);
            writeSpawnEggEffects(list);
            this.closeModal();
            this.init();
        }, this::closeModal));
    }

    private void removeAttribute(AttributeEntry entry) {
        List<AttributeEntry> list = new ArrayList<>(ItemEditorCompat.readAttributes(this.currentTag));
        list.removeIf(candidate -> candidate.curio() == entry.curio() && candidate.index() == entry.index());
        ItemEditorCompat.writeAttributes(this.currentTag, list);
        syncWorkingStackTag();
    }

    private void removeEffect(EffectEntry entry) {
        EffectCategory category = effectStorageCategory();
        if (entry != null && category.isPotion() && isSyntheticPotionEffect(entry)) {
            return;
        }
        if (entry != null && entry.category().isPotion()) {
            category = EffectCategory.POTION;
        }
        List<EffectEntry> list = new ArrayList<>(ItemEditorCompat.readEffects(this.currentTag, category));
        list.removeIf(candidate -> candidate.index() == entry.index());
        ItemEditorCompat.writeEffects(this.currentTag, category, list);
        syncWorkingStackTag();
    }

    private void removeSpawnEggAttribute(int index) {
        List<SpawnEggAttributeEntry> list = new ArrayList<>(
                ItemEditorCompat.readSpawnEggAttributes(spawnEggEntityTag()));
        list.removeIf(candidate -> candidate.index() == index);
        writeSpawnEggAttributes(list);
    }

    private void removeSpawnEggEffect(int index) {
        List<SpawnEggEffectEntry> list = new ArrayList<>(ItemEditorCompat.readSpawnEggEffects(spawnEggEntityTag()));
        list.removeIf(candidate -> candidate.index() == index);
        writeSpawnEggEffects(list);
    }

    private CompoundTag spawnEggEntityTag() {
        return ItemEditorCompat.spawnEggEntityTag(this.currentTag);
    }

    private void writeSpawnEggEntityTag(CompoundTag entityTag) {
        ItemEditorCompat.setSpawnEggEntityTag(this.currentTag, entityTag);
        syncWorkingStackTag();
    }

    private void writeSpawnEggAttributes(List<SpawnEggAttributeEntry> entries) {
        CompoundTag entityTag = spawnEggEntityTag();
        ItemEditorCompat.writeSpawnEggAttributes(entityTag, entries);
        writeSpawnEggEntityTag(entityTag);
    }

    private void writeSpawnEggEffects(List<SpawnEggEffectEntry> entries) {
        CompoundTag entityTag = spawnEggEntityTag();
        ItemEditorCompat.writeSpawnEggEffects(entityTag, entries);
        writeSpawnEggEntityTag(entityTag);
    }

    private String spawnEggLootTableValue() {
        CompoundTag entityTag = spawnEggEntityTag();
        if (entityTag.contains("DeathLootTable", Tag.TAG_STRING)) {
            return entityTag.getString("DeathLootTable");
        }
        return ItemEditorCompat.spawnEggDefaultLootTable(this.workingStack, this.currentTag);
    }

    private void setSpawnEggLootTable(String value) {
        CompoundTag entityTag = spawnEggEntityTag();
        String next = value == null || value.isBlank() ? "minecraft:empty" : ItemEditorCompat.normalizeId(value);
        entityTag.putString("DeathLootTable", next);
        writeSpawnEggEntityTag(entityTag);
    }

    private boolean isSpawnEggEditor() {
        return ItemEditorCompat.isSpawnEgg(this.workingStack);
    }

    private void toggleSpawnEggFlag(String key) {
        CompoundTag entityTag = spawnEggEntityTag();
        boolean enabled = ItemEditorCompat.isSpawnEggFlagEnabled(entityTag, key);
        ItemEditorCompat.setSpawnEggFlag(entityTag, key, !enabled);
        writeSpawnEggEntityTag(entityTag);
    }

    private void toggleSpawnEggBaby() {
        CompoundTag entityTag = spawnEggEntityTag();
        ItemEditorCompat.setSpawnEggBaby(entityTag, !ItemEditorCompat.isSpawnEggBaby(entityTag));
        writeSpawnEggEntityTag(entityTag);
    }

    private ItemStack spawnEggEquipmentStack(CompoundTag entityTag, SpawnEggEquipmentSlot slot) {
        ListTag list = entityTag.getList(slot.itemListKey(), Tag.TAG_COMPOUND);
        if (slot.index() < 0 || slot.index() >= list.size()) {
            return ItemStack.EMPTY;
        }
        CompoundTag compound = list.getCompound(slot.index());
        return compound.isEmpty() ? ItemStack.EMPTY : ItemStack.of(compound.copy());
    }

    private float spawnEggEquipmentChance(CompoundTag entityTag, SpawnEggEquipmentSlot slot) {
        ListTag list = entityTag.getList(slot.dropListKey(), Tag.TAG_FLOAT);
        if (slot.index() < 0 || slot.index() >= list.size()) {
            return 0.0F;
        }
        return Mth.clamp(list.getFloat(slot.index()), 0.0F, 1.0F);
    }

    private void setSpawnEggEquipmentChance(SpawnEggEquipmentSlot slot, String raw) {
        try {
            float chance = raw == null || raw.isBlank() ? 0.0F : Mth.clamp(Float.parseFloat(raw.trim()), 0.0F, 1.0F);
            CompoundTag entityTag = spawnEggEntityTag();
            writeSpawnEggEquipmentChanceGroup(entityTag, slot, chance);
            writeSpawnEggEntityTag(entityTag);
        } catch (Exception ignored) {
        }
    }

    private void setSpawnEggEquipmentStack(SpawnEggEquipmentSlot slot, ItemStack stack) {
        CompoundTag entityTag = spawnEggEntityTag();
        ListTag list = entityTag.getList(slot.itemListKey(), Tag.TAG_COMPOUND);
        ensureIndexWithEmptyCompound(list, slot.index());
        list.set(slot.index(), stack == null || stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag()));
        entityTag.put(slot.itemListKey(), list);
        writeSpawnEggEntityTag(entityTag);
    }

    private void clearSpawnEggEquipmentStack(SpawnEggEquipmentSlot slot) {
        setSpawnEggEquipmentStack(slot, ItemStack.EMPTY);
    }

    private static void ensureIndexWithEmptyCompound(ListTag list, int index) {
        while (list.size() <= index) {
            list.add(new CompoundTag());
        }
    }

    private static void ensureIndexWithZeroFloat(ListTag list, int index) {
        while (list.size() <= index) {
            list.add(net.minecraft.nbt.FloatTag.valueOf(0.0F));
        }
    }

    private void writeSpawnEggEquipmentChanceGroup(CompoundTag entityTag, SpawnEggEquipmentSlot slot, float chance) {
        ListTag existing = entityTag.getList(slot.dropListKey(), Tag.TAG_FLOAT);
        ListTag updated = new ListTag();
        for (SpawnEggEquipmentSlot candidate : SpawnEggEquipmentSlot.ORDERED) {
            if (!candidate.dropListKey().equals(slot.dropListKey())) {
                continue;
            }
            float value = candidate == slot ? chance : readDropChance(existing, candidate.index());
            updated.add(net.minecraft.nbt.FloatTag.valueOf(Mth.clamp(value, 0.0F, 1.0F)));
        }
        entityTag.put(slot.dropListKey(), updated);
    }

    private static float readDropChance(ListTag list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return 0.0F;
        }
        return list.getFloat(index);
    }

    private static String formatDropChance(float chance) {
        return String.format(java.util.Locale.ROOT, "%.2f", Mth.clamp(chance, 0.0F, 1.0F));
    }

    private enum SpawnEggEquipmentSlot {
        MAINHAND("Main Hand", "HandItems", "HandDropChances", 0),
        OFFHAND("Offhand", "HandItems", "HandDropChances", 1),
        BOOTS("Boots", "ArmorItems", "ArmorDropChances", 0),
        LEGGINGS("Leggings", "ArmorItems", "ArmorDropChances", 1),
        CHESTPLATE("Chestplate", "ArmorItems", "ArmorDropChances", 2),
        HELMET("Helmet", "ArmorItems", "ArmorDropChances", 3);

        static final SpawnEggEquipmentSlot[] ORDERED = values();

        private final String label;
        private final String itemListKey;
        private final String dropListKey;
        private final int index;

        SpawnEggEquipmentSlot(String label, String itemListKey, String dropListKey, int index) {
            this.label = label;
            this.itemListKey = itemListKey;
            this.dropListKey = dropListKey;
            this.index = index;
        }

        String label() {
            return this.label;
        }

        String itemListKey() {
            return this.itemListKey;
        }

        String dropListKey() {
            return this.dropListKey;
        }

        int index() {
            return this.index;
        }
    }

    private void syncWorkingStackTag() {
        this.workingStack.setTag(this.currentTag.isEmpty() ? null : this.currentTag.copy());
    }

    private void save() {
        List<EnchantmentEntry> enchantments = ItemEditorCompat.readEnchantments(this.workingStack, this.currentTag);
        ItemEditorCompat.writeEnchantments(this.workingStack, this.currentTag, enchantments);
        ItemEditorNetwork.sendToServer(new SaveItemEditorPacket(this.workingStack, this.containerId, this.slotIndex));
        this.minecraft.setScreen(null);
    }

    private static String enchantmentDisplayName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(ItemEditorCompat.normalizeId(id));
        if (key == null) {
            return id == null ? "unknown" : id;
        }
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(key);
        if (enchantment == null) {
            return id == null || id.isBlank() ? "unknown" : id;
        }
        Component translated = Component.translatable(enchantment.getDescriptionId());
        String text = translated.getString();
        return text == null || text.isBlank() ? key.toString() : text;
    }

    private BabelWidget hiddenFlagRow(HideFlagOption left, HideFlagOption right) {
        BabelRow row = new BabelRow().fillWidth().gap(6);
        row.add(hiddenFlagButton(left));
        if (right != null) {
            row.add(hiddenFlagButton(right));
        } else {
            row.add(BabelUi.spacer(0, 1).flexGrow(1));
        }
        return row;
    }

    private BabelButton hiddenFlagButton(HideFlagOption option) {
        boolean enabled = isHiddenFlagSet(option);
        BabelButton button = new BabelButton(Component.translatable(option.labelKey()), () -> {
            toggleHiddenFlag(option);
            this.init();
        });
        button.fillWidth();
        button.height(22);
        button.colors(enabled ? BabelTheme.SUCCESS : BabelTheme.DANGER, enabled ? 0xFF87D989 : 0xFFFF8080,
                BabelTheme.TEXT);
        return button;
    }

    private boolean isHiddenFlagSet(HideFlagOption option) {
        return (getCurrentHideFlags(option.tagName()) & option.value()) != 0;
    }

    private int getCurrentHideFlags(String tagName) {
        return this.currentTag.contains(tagName, Tag.TAG_ANY_NUMERIC) ? this.currentTag.getInt(tagName) : 0;
    }

    private void setCurrentHideFlags(String tagName, int flags) {
        if (flags <= 0) {
            this.currentTag.remove(tagName);
        } else {
            this.currentTag.putInt(tagName, flags);
        }
        syncWorkingStackTag();
    }

    private void toggleHiddenFlag(HideFlagOption option) {
        int flags = getCurrentHideFlags(option.tagName());
        flags ^= option.value();
        setCurrentHideFlags(option.tagName(), flags);
    }

    private void clearHiddenFlags() {
        setCurrentHideFlags("HideFlags", 0);
        setCurrentHideFlags(SF_HIDE_EFFECT_FLAGS_TAG, 0);
    }

}
