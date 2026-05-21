package com.gabri.itemeditor.client;

import com.gabri.itemeditor.ItemEditor;
import com.gabri.itemeditor.ItemEditorClientHooks;
import com.gabri.itemeditor.client.screen.LootTableSelectionModal;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ItemEditor.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ItemEditorClientSetup {
    private ItemEditorClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemEditorClientHooks.OPEN_EDITOR = ItemEditorClient::open;
        ItemEditorClientHooks.APPLY_LOOT_TABLES = LootTableSelectionModal::applyServerLootTables;
    }
}
