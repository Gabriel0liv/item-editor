package com.gabri.itemeditor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ItemEditorClientHooks {
    public interface OpenEditorHook {
        void open(CompoundTag fullItemNbt, int containerId, int slotIndex);
    }

    public interface LootTableHook {
        void apply(List<ResourceLocation> lootTables);
    }

    public static OpenEditorHook OPEN_EDITOR = (fullItemNbt, containerId, slotIndex) -> {};
    public static LootTableHook APPLY_LOOT_TABLES = lootTables -> {};

    private ItemEditorClientHooks() {
    }
}
