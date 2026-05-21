package com.gabri.itemeditor.network;

import com.gabri.itemeditor.editor.ItemEditorCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestOpenItemEditorPacket {
    private final int containerId;
    private final int slotIndex;

    public RequestOpenItemEditorPacket(int containerId, int slotIndex) {
        this.containerId = containerId;
        this.slotIndex = slotIndex;
    }

    public RequestOpenItemEditorPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readInt();
        this.slotIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.containerId);
        buf.writeInt(this.slotIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context == null) {
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!sender.hasPermissions(2)) {
                sender.sendSystemMessage(Component.translatable("itemeditor.command.access_denied"));
                return;
            }

            ItemStack stack = resolveTargetStack(sender);
            if (stack.isEmpty()) {
                sender.sendSystemMessage(Component.translatable("itemeditor.command.hold_item_or_slot"));
                return;
            }

            ItemEditorNetwork.sendToPlayer(sender, new OpenItemEditorPacket(stack, this.containerId, this.slotIndex));
        });
        context.setPacketHandled(true);
    }

    private ItemStack resolveTargetStack(ServerPlayer sender) {
        boolean explicitSlot = this.containerId >= 0 && this.slotIndex >= 0;
        if (explicitSlot) {
            if (sender.containerMenu != null && sender.containerMenu.containerId == this.containerId && this.slotIndex < sender.containerMenu.slots.size()) {
                ItemStack stack = sender.containerMenu.slots.get(this.slotIndex).getItem();
                return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
            return ItemStack.EMPTY;
        }

        ItemStack handStack = sender.getMainHandItem();
        return handStack == null || handStack.isEmpty() ? ItemStack.EMPTY : handStack.copy();
    }
}
