package com.gabri.itemeditor.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("all")
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private static void itemeditor$onLoad(CompoundTag nbt, CallbackInfoReturnable<MobEffectInstance> cir) {
        String idStr = null;
        if (nbt.contains("IdString", Tag.TAG_STRING)) {
            idStr = nbt.getString("IdString");
        } else if (nbt.contains("Id", Tag.TAG_STRING)) {
            idStr = nbt.getString("Id");
        }
        if (idStr == null || idStr.isBlank()) {
            return;
        }

        ResourceLocation rl = ResourceLocation.tryParse(idStr);
        if (rl == null) {
            return;
        }

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
        if (effect == null) {
            return;
        }

        int amplifier = nbt.getByte("Amplifier");
        int duration = nbt.getInt("Duration");
        boolean ambient = nbt.getBoolean("Ambient");
        boolean visible = nbt.contains("ShowParticles") ? nbt.getBoolean("ShowParticles") : true;
        boolean showIcon = nbt.contains("ShowIcon") ? nbt.getBoolean("ShowIcon") : visible;

        MobEffectInstance inst = new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon);

        if (nbt.contains("HiddenEffect", Tag.TAG_COMPOUND)) {
            MobEffectInstance hidden = MobEffectInstance.load(nbt.getCompound("HiddenEffect"));
            ((MobEffectInstanceAccessor) inst).itemeditor$setHiddenEffect(hidden);
        }

        cir.setReturnValue(inst);
    }
}
