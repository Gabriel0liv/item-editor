package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import com.gabri.itemeditor.client.widget.LootTableChoiceWidget;
import com.gabri.itemeditor.network.ItemEditorNetwork;
import com.gabri.itemeditor.network.RequestLootTableListPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class LootTableSelectionModal extends BabelPanel {
    private static final ResourceLocation EMPTY_LOOT_TABLE = new ResourceLocation("minecraft", "empty");
    private static final int MAX_VISIBLE_RESULTS = 250;
    private static final List<ResourceLocation> SERVER_LOOT_TABLES = new ArrayList<>();
    private static LootTableSelectionModal activeModal;
    private enum LootTableTypeFilter {
        ALL("itemeditor.ui.loot_type.all"),
        CHESTS("itemeditor.ui.loot_type.chests"),
        ENTITIES("itemeditor.ui.loot_type.entities"),
        GAMEPLAY("itemeditor.ui.loot_type.gameplay"),
        ARCHAEOLOGY("itemeditor.ui.loot_type.archaeology"),
        GIFT("itemeditor.ui.loot_type.gift"),
        PIGLIN_BARTERING("itemeditor.ui.loot_type.piglin_bartering"),
        TRIALS("itemeditor.ui.loot_type.trials");

        private static final LootTableTypeFilter[] ORDERED = values();
        private final String labelKey;

        LootTableTypeFilter(String labelKey) {
            this.labelKey = labelKey;
        }

        String labelKey() {
            return this.labelKey;
        }

        LootTableTypeFilter next() {
            return ORDERED[(this.ordinal() + 1) % ORDERED.length];
        }
    }

    private final Consumer<String> onSelect;
    private final Runnable onCancel;
    private final List<ResourceLocation> allLootTables = new ArrayList<>();
    private final List<ResourceLocation> filteredLootTables = new ArrayList<>();
    private final BabelTextField searchField;
    private final BabelButton typeButton;
    private final BabelScrollPane listPane;
    private final BabelLabel pageLabel;
    private final BabelButton prevButton;
    private final BabelButton nextButton;
    private final String selectedValue;
    private LootTableTypeFilter selectedType = LootTableTypeFilter.ALL;
    private int currentPage = 0;

    public LootTableSelectionModal(Font font, String selected, Consumer<String> onSelect, Runnable onCancel) {
        this.onSelect = onSelect == null ? value -> {
        } : value -> {
            clearActiveModal();
            onSelect.accept(value);
        };
        this.onCancel = onCancel == null ? () -> {
        } : () -> {
            clearActiveModal();
            onCancel.run();
        };
        this.selectedValue = selected == null ? "" : selected.trim();
        setActiveModal(this);

        refreshLootTables();

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int modalWidth = Math.max(320, Math.min(560, screenWidth - 32));
        int listHeight = Math.max(120, Math.min(240, screenHeight - 128));

        style().width(modalWidth);
        style().autoHeight();
        style().padding(10);
        style().gap(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);

        BabelLabel title = new BabelLabel(Component.translatable("itemeditor.ui.select_loot_table"));
        title.style().textColor(BabelTheme.TEXT);
        add(title);

        this.searchField = new BabelTextField("", value -> {
            this.currentPage = 0;
            filter(value);
            rebuild();
        });
        this.searchField.placeholder(Component.translatable("itemeditor.ui.search_loot_tables"));
        this.searchField.maxLength(64).fillWidth();

        BabelRow filterRow = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        filterRow.add(this.searchField);
        this.typeButton = new BabelButton(Component.translatable(this.selectedType.labelKey()), this::cycleLootType)
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        filterRow.add(this.typeButton);
        add(filterRow);

        this.listPane = new BabelScrollPane().fillWidth().height(listHeight);
        this.listPane.showScrollbar(true);
        add(this.listPane);

        BabelRow pageRow = new BabelRow().fillWidth().gap(6).align(BabelAlign.CENTER);
        this.prevButton = new BabelButton(Component.translatable("itemeditor.ui.prev"), this::previousPage)
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        this.pageLabel = new BabelLabel(Component.empty());
        this.pageLabel.style().textColor(BabelTheme.TEXT_MUTED);
        this.pageLabel.style().flexGrow(1);
        this.pageLabel.align(BabelAlign.CENTER);
        this.nextButton = new BabelButton(Component.translatable("itemeditor.ui.next"), this::nextPage)
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT);
        pageRow.add(this.prevButton);
        pageRow.add(this.pageLabel);
        pageRow.add(this.nextButton);
        add(pageRow);

        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(new BabelButton(Component.translatable("itemeditor.ui.cancel"), this.onCancel)
                .colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT));
        add(actions);

        rebuild();
        requestServerLootTables();
    }

    private void filter(String query) {
        filteredLootTables.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> prefixMatches = new ArrayList<>();
        List<ResourceLocation> containsMatches = new ArrayList<>();
        for (ResourceLocation id : allLootTables) {
            if (!matchesType(id)) {
                continue;
            }
            String text = id.toString().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                filteredLootTables.add(id);
            } else if (text.startsWith(normalized)) {
                prefixMatches.add(id);
            } else if (text.contains(normalized)) {
                containsMatches.add(id);
            }
        }
        filteredLootTables.addAll(prefixMatches);
        filteredLootTables.addAll(containsMatches);
        if (this.currentPage < 0) {
            this.currentPage = 0;
        }
        int totalPages = totalPages();
        if (totalPages > 0 && this.currentPage >= totalPages) {
            this.currentPage = totalPages - 1;
        }
    }

    private void rebuild() {
        listPane.clear();
        String normalizedSelected = this.selectedValue;
        updatePageControls();
        if (filteredLootTables.isEmpty()) {
            listPane.add(BabelTemplates.emptyState(Component.translatable("itemeditor.ui.no_loot_tables"),
                    Component.translatable("itemeditor.ui.no_loot_tables_desc")));
            return;
        }
        int startIndex = currentPage * MAX_VISIBLE_RESULTS;
        int endIndex = Math.min(startIndex + MAX_VISIBLE_RESULTS, filteredLootTables.size());
        if (filteredLootTables.size() > MAX_VISIBLE_RESULTS) {
            BabelLabel notice = new BabelLabel(Component.translatable(
                    "itemeditor.ui.loot_table_page",
                    currentPage + 1,
                    totalPages(),
                    filteredLootTables.size()));
            notice.style().textColor(BabelTheme.TEXT_MUTED);
            notice.wrap(true);
            listPane.add(notice);
        }
        for (ResourceLocation lootTable : filteredLootTables.subList(startIndex, endIndex)) {
            boolean isSelected = lootTable.toString().equalsIgnoreCase(normalizedSelected);
            LootTableChoiceWidget row = new LootTableChoiceWidget(Component.literal(lootTable.toString()),
                    Component.empty(), () -> onSelect.accept(lootTable.toString())).selected(isSelected);
            listPane.add(row);
        }
    }

    private static ResourceLocation normalizeLootTableId(ResourceLocation key) {
        String path = key.getPath();
        if (path.startsWith("loot_tables/")) {
            path = path.substring("loot_tables/".length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        return new ResourceLocation(key.getNamespace(), path);
    }

    private static void collectSingleplayerLootTables(Set<ResourceLocation> collected) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        try {
            server.getLootData().getKeys(net.minecraft.world.level.storage.loot.LootDataType.TABLE).stream()
                    .filter(id -> !isBlockLootTable(id))
                    .forEach(collected::add);
        } catch (Exception ignored) {
        }
    }

    private static void collectCachedServerLootTables(Set<ResourceLocation> collected) {
        collected.addAll(SERVER_LOOT_TABLES);
    }

    private void refreshLootTables() {
        Set<ResourceLocation> collected = new java.util.LinkedHashSet<>();
        collected.add(EMPTY_LOOT_TABLE);
        collectSingleplayerLootTables(collected);
        collectCachedServerLootTables(collected);
        allLootTables.clear();
        allLootTables.addAll(collected);
        allLootTables.sort(Comparator.comparing(ResourceLocation::toString, String.CASE_INSENSITIVE_ORDER));
        if (!allLootTables.isEmpty() && !allLootTables.get(0).equals(EMPTY_LOOT_TABLE)) {
            allLootTables.removeIf(EMPTY_LOOT_TABLE::equals);
            allLootTables.add(0, EMPTY_LOOT_TABLE);
        }
        filteredLootTables.clear();
        filteredLootTables.addAll(allLootTables);
    }

    private void cycleLootType() {
        this.selectedType = this.selectedType.next();
        this.typeButton.text(Component.translatable(this.selectedType.labelKey()));
        this.currentPage = 0;
        filter(this.searchField.value());
        rebuild();
    }

    private void previousPage() {
        if (this.currentPage <= 0) {
            return;
        }
        this.currentPage--;
        rebuild();
    }

    private void nextPage() {
        int totalPages = totalPages();
        if (this.currentPage >= totalPages - 1) {
            return;
        }
        this.currentPage++;
        rebuild();
    }

    private int totalPages() {
        if (this.filteredLootTables.isEmpty()) {
            return 0;
        }
        return (this.filteredLootTables.size() + MAX_VISIBLE_RESULTS - 1) / MAX_VISIBLE_RESULTS;
    }

    private void updatePageControls() {
        int totalPages = totalPages();
        boolean hasPages = totalPages > 1;
        this.pageLabel.text(Component.translatable("itemeditor.ui.loot_table_page_label",
                totalPages == 0 ? 0 : this.currentPage + 1,
                totalPages));
        this.prevButton.colors(this.currentPage <= 0 ? BabelTheme.PANEL_ALT : BabelTheme.PANEL_ALT,
                this.currentPage <= 0 ? BabelTheme.ACCENT : BabelTheme.ACCENT,
                this.currentPage <= 0 ? 0xFF808080 : BabelTheme.TEXT);
        this.nextButton.colors(this.currentPage >= totalPages - 1 ? BabelTheme.PANEL_ALT : BabelTheme.PANEL_ALT,
                this.currentPage >= totalPages - 1 ? BabelTheme.ACCENT : BabelTheme.ACCENT,
                this.currentPage >= totalPages - 1 ? 0xFF808080 : BabelTheme.TEXT);
    }

    private boolean matchesType(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (this.selectedType == LootTableTypeFilter.ALL) {
            return true;
        }
        if (EMPTY_LOOT_TABLE.equals(id)) {
            return true;
        }
        String path = id.getPath();
        return switch (this.selectedType) {
            case CHESTS -> path.startsWith("chests/");
            case ENTITIES -> path.startsWith("entities/");
            case GAMEPLAY -> path.startsWith("gameplay/");
            case ARCHAEOLOGY -> path.startsWith("archaeology/");
            case GIFT -> path.startsWith("gift/");
            case PIGLIN_BARTERING -> path.startsWith("piglin_bartering/");
            case TRIALS -> path.startsWith("trial_spawner/") || path.startsWith("trial_chamber/");
            default -> true;
        };
    }

    private static void setActiveModal(LootTableSelectionModal modal) {
        activeModal = modal;
    }

    private static void clearActiveModal() {
        activeModal = null;
    }

    private void requestServerLootTables() {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        ItemEditorNetwork.sendToServer(new RequestLootTableListPacket());
    }

    public static void applyServerLootTables(List<ResourceLocation> lootTables) {
        SERVER_LOOT_TABLES.clear();
        if (lootTables != null && !lootTables.isEmpty()) {
            for (ResourceLocation lootTable : lootTables) {
                if (lootTable != null && !isBlockLootTable(lootTable)) {
                    SERVER_LOOT_TABLES.add(lootTable);
                }
            }
        }
        LootTableSelectionModal modal = activeModal;
        if (modal != null) {
            modal.refreshLootTables();
            modal.filter(modal.searchField.value());
            modal.rebuild();
        }
    }

    private static boolean isBlockLootTable(ResourceLocation id) {
        return id != null && id.getPath().startsWith("blocks/");
    }
}
