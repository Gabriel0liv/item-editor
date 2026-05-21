package com.gabri.itemeditor.mixin;

import com.gabri.itemeditor.editor.ItemEditorCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("all")
@Mixin(ItemStack.class)
public abstract class ItemStackPersistenceMixin {

    @Inject(method = "copy", at = @At("RETURN"))
    private void itemeditor$onCopy(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack copy = cir.getReturnValue();
        if (copy != null && !copy.isEmpty() && copy.hasTag()) {
            ItemEditorCompat.syncPersistentBackups(copy, copy.getTag());
        }
    }

    @Inject(method = "copyWithCount", at = @At("RETURN"))
    private void itemeditor$onCopyWithCount(int count, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack copy = cir.getReturnValue();
        if (copy != null && !copy.isEmpty() && copy.hasTag()) {
            ItemEditorCompat.syncPersistentBackups(copy, copy.getTag());
        }
    }

    @Inject(method = "split", at = @At("RETURN"))
    private void itemeditor$onSplit(int amount, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack split = cir.getReturnValue();
        if (split != null && !split.isEmpty() && split.hasTag()) {
            ItemEditorCompat.syncPersistentBackups(split, split.getTag());
        }
    }

    @Inject(method = "setTag", at = @At("HEAD"))
    private void itemeditor$onSetTag(CompoundTag nbt, CallbackInfo ci) {
        if (nbt != null) {
            ItemEditorCompat.syncPersistentBackups((ItemStack) (Object) this, nbt);
        }
    }

    @Inject(method = "of", at = @At("RETURN"))
    private static void itemeditor$onLoad(CompoundTag nbt, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack != null && !stack.isEmpty() && stack.hasTag()) {
            ItemEditorCompat.syncPersistentBackups(stack, stack.getTag());
        }
    }
}
