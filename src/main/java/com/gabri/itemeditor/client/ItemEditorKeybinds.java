package com.gabri.itemeditor.client;

import com.gabri.itemeditor.ItemEditor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ItemEditor.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ItemEditorKeybinds {
    private static final String CATEGORY = "key.categories.itemeditor";
    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.itemeditor.open_editor",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    static {
        OPEN_EDITOR.setKeyConflictContext(new ItemEditorKeyConflictContext());
    }

    private ItemEditorKeybinds() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
    }

    private static final class ItemEditorKeyConflictContext implements IKeyConflictContext {
        @Override
        public boolean isActive() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player != null && (minecraft.screen == null || minecraft.screen instanceof AbstractContainerScreen<?>);
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return this == other || other == KeyConflictContext.IN_GAME;
        }
    }
}
