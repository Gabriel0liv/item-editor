package com.gabri.itemeditor.network;

import com.gabri.itemeditor.editor.ItemEditorCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveItemEditorPacket {
    private final CompoundTag fullItemNbt;
    private final int containerId;
    private final int slotIndex;

    public SaveItemEditorPacket(ItemStack stack, int containerId, int slotIndex) {
        this.fullItemNbt = stack == null || stack.isEmpty() ? new CompoundTag() : stack.copy().save(new CompoundTag());
        this.containerId = containerId;
        this.slotIndex = slotIndex;
    }

    public SaveItemEditorPacket(FriendlyByteBuf buf) {
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

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!sender.hasPermissions(2)) {
                sender.sendSystemMessage(Component.translatable("itemeditor.command.access_denied"));
                return;
            }

            ItemStack newStack = ItemEditorCompat.fromFullNbt(this.fullItemNbt);
            if (newStack.isEmpty()) {
                sender.sendSystemMessage(Component.translatable("itemeditor.command.invalid_payload"));
                return;
            }

            boolean updated = false;
            boolean explicitSlot = this.containerId >= 0 && this.slotIndex >= 0;
            if (explicitSlot) {
                if (sender.containerMenu != null && sender.containerMenu.containerId == this.containerId && this.slotIndex < sender.containerMenu.slots.size()) {
                    var slot = sender.containerMenu.slots.get(this.slotIndex);
                    ItemStack sanitized = sanitizeForSlot(newStack, slot.getItem().isEmpty() ? newStack : slot.getItem(), slot.getMaxStackSize());
                    slot.set(sanitized);
                    slot.setChanged();
                    sender.containerMenu.broadcastChanges();
                    sender.inventoryMenu.broadcastChanges();
                    updated = true;
                }
            } else {
                ItemStack handStack = newStack.copy();
                clampStackValues(handStack, Math.max(1, handStack.getMaxStackSize()));
                sender.setItemInHand(InteractionHand.MAIN_HAND, handStack);
                sender.containerMenu.broadcastChanges();
                sender.inventoryMenu.broadcastChanges();
                updated = true;
            }

            if (updated) {
                sender.sendSystemMessage(Component.translatable("itemeditor.command.saved"));
            } else {
                sender.sendSystemMessage(Component.translatable("itemeditor.command.save_failed_target"));
            }
        });
        context.setPacketHandled(true);
    }

    private static ItemStack sanitizeForSlot(ItemStack source, ItemStack slotStack, int slotMax) {
        ItemStack stack = source.copy();
        int maxCount = Math.max(1, slotMax);
        clampStackValues(stack, maxCount);
        if (slotStack != null && !slotStack.isEmpty() && stack.getItem() != slotStack.getItem()) {
            clampStackValues(stack, Math.min(maxCount, stack.getMaxStackSize()));
        }
        return stack;
    }

    private static void clampStackValues(ItemStack stack, int maxCount) {
        int clampedCount = Mth.clamp(stack.getCount(), 1, Math.max(1, maxCount));
        stack.setCount(clampedCount);
        if (stack.isDamageableItem()) {
            int maxDamage = Math.max(0, stack.getMaxDamage());
            stack.setDamageValue(Mth.clamp(stack.getDamageValue(), 0, maxDamage));
        }
    }
}
