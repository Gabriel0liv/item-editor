package com.gabri.itemeditor.client;

import com.gabri.itemeditor.ItemEditor;
import com.gabri.itemeditor.network.ItemEditorNetwork;
import com.gabri.itemeditor.network.RequestOpenItemEditorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ItemEditor.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemEditorKeybindHandler {
    private static final Field HOVERED_SLOT;

    static {
        Field field;
        try {
            field = ObfuscationReflectionHelper.findField(AbstractContainerScreen.class, "hoveredSlot");
        } catch (Exception ignored) {
            field = null;
        }
        HOVERED_SLOT = field;
    }

    private ItemEditorKeybindHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (ItemEditorKeybinds.OPEN_EDITOR.consumeClick()) {
            handleOpenEditor();
        }
    }

    private static void handleOpenEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int containerId = -1;
        int slotIndex = -1;
        Slot hoveredSlot = resolveHoveredSlot(minecraft.screen);
        if (hoveredSlot != null && hoveredSlot.hasItem() && minecraft.screen instanceof AbstractContainerScreen<?> containerScreen) {
            containerId = containerScreen.getMenu().containerId;
            slotIndex = hoveredSlot.index;
        }

        ItemEditorNetwork.sendToServer(new RequestOpenItemEditorPacket(containerId, slotIndex));
    }

    private static Slot resolveHoveredSlot(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return null;
        }
        if (HOVERED_SLOT == null) {
            return null;
        }

        try {
            return (Slot) HOVERED_SLOT.get(containerScreen);
        } catch (Exception ignored) {
            return null;
        }
    }
}
