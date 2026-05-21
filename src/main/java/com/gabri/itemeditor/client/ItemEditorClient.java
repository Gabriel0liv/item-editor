package com.gabri.itemeditor.client;

import com.gabri.itemeditor.client.screen.ItemEditorScreen;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ItemEditorClient {
    private ItemEditorClient() {
    }

    public static void open(CompoundTag fullItemNbt, int containerId, int slotIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        ItemStack stack = ItemEditorCompat.fromFullNbt(fullItemNbt);
        minecraft.setScreen(new ItemEditorScreen(stack, containerId, slotIndex));
    }
}
