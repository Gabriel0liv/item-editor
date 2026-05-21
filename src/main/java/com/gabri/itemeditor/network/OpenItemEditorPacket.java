package com.gabri.itemeditor.network;

import com.gabri.itemeditor.client.ItemEditorClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenItemEditorPacket {
    private final CompoundTag fullItemNbt;
    private final int containerId;
    private final int slotIndex;

    public OpenItemEditorPacket(ItemStack stack, int containerId, int slotIndex) {
        this.fullItemNbt = stack == null || stack.isEmpty() ? new CompoundTag() : stack.copy().save(new CompoundTag());
        this.containerId = containerId;
        this.slotIndex = slotIndex;
    }

    public OpenItemEditorPacket(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        this.fullItemNbt = tag == null ? new CompoundTag() : tag;
        this.containerId = buf.readInt();
        this.slotIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.fullItemNbt);
        buf.writeInt(this.containerId);
        buf.writeInt(this.slotIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context == null) {
            return;
        }
        context.enqueueWork(() -> ItemEditorClient.open(this.fullItemNbt, this.containerId, this.slotIndex));
        context.setPacketHandled(true);
    }
}
