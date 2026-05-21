package com.gabri.itemeditor.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
    @Accessor("hiddenEffect")
    void itemeditor$setHiddenEffect(@Nullable MobEffectInstance hiddenEffect);
}
