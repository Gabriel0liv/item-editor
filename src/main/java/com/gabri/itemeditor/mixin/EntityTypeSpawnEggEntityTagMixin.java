package com.gabri.itemeditor.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EntityType.class)
public abstract class EntityTypeSpawnEggEntityTagMixin {

    @Inject(
            method = "updateCustomEntityTag(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL"))
    private static void itemeditor$reapplySpawnEggEntityTag(Level level, Player player, Entity entity,
                                                            CompoundTag stackTag, CallbackInfo ci) {
        if (level == null || level.isClientSide() || entity == null || stackTag == null || stackTag.isEmpty()) {
            return;
        }

        if (!stackTag.contains("EntityTag", 10)) {
            return;
        }

        CompoundTag customEntityTag = stackTag.getCompound("EntityTag");
        if (customEntityTag.isEmpty()) {
            return;
        }

        CompoundTag merged = entity.saveWithoutId(new CompoundTag());
        UUID uuid = entity.getUUID();
        merged.merge(customEntityTag.copy());
        entity.load(merged);
        entity.setUUID(uuid);
    }
}
