package com.gabri.itemeditor.events;

import com.gabri.itemeditor.ItemEditor;
import com.gabri.itemeditor.editor.ItemEditorCompat;
import com.gabri.babel.core.util.BabelCuriosSupport;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("all")
@Mod.EventBusSubscriber(modid = ItemEditor.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemEffectModifierHandler {
    private static final String MAIN_TAG = ItemEditorCompat.EFFECTS_TAG;
    private static final String ON_HIT = ItemEditorCompat.EffectCategory.ON_HIT.tagKey();
    private static final String ON_HURT = ItemEditorCompat.EffectCategory.ON_HURT.tagKey();
    private static final String ON_USE = ItemEditorCompat.EffectCategory.ON_USE.tagKey();
    private static final String ON_EQUIP = ItemEditorCompat.EffectCategory.ON_EQUIP.tagKey();
    private static final String ON_USE_COLOR = "on_use_color";
    private static final String ON_USE_MIRROR_TAG = "on_use_potion_mirror";
    private static final int ON_EQUIP_REFRESH_INTERVAL = 20;
    private static final int ON_EQUIP_APPLY_DURATION = 210;
    private static final Random RANDOM = new Random();
    private static final java.util.Map<String, MobEffect> EFFECT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private ItemEffectModifierHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player == null || player.level().isClientSide()) {
            return;
        }

        if (player.tickCount % ON_EQUIP_REFRESH_INTERVAL != 0) {
            return;
        }

        applyOnEquipFromStack(player, player.getMainHandItem(), EquipmentSlot.MAINHAND.getName());
        applyOnEquipFromStack(player, player.getOffhandItem(), EquipmentSlot.OFFHAND.getName());

        for (ItemStack armorStack : player.getArmorSlots()) {
            EquipmentSlot armorSlot = LivingEntity.getEquipmentSlotForItem(armorStack);
            String slotName = armorSlot != null ? armorSlot.getName() : "any";
            applyOnEquipFromStack(player, armorStack, slotName);
        }

        if (ModList.get().isLoaded("curios")) {
            applyCurioEquipEffects(player);
        }
    }

    @SubscribeEvent
    public static void onLivingCuriosEvent(LivingEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }

        if (!ModList.get().isLoaded("curios")) {
            return;
        }

        BabelCuriosSupport.forEachCurioEventStack(event, (slotId, index, wearer, curioStack) -> {
            if (curioStack == null || curioStack.isEmpty() || !curioStack.hasTag()) {
                return;
            }
            applyOnEquipFromStack(wearer, curioStack, slotId);
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }

        Entity source = event.getSource().getEntity();
        LivingEntity target = event.getEntity();

        if (source instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.isEmpty() && weapon.hasTag()) {
                processEffects(weapon, ON_HIT, attacker, target, null);
            }
        }

        if (target != null) {
            LivingEntity attacker = source instanceof LivingEntity ? (LivingEntity) source : null;
            applyOnHurtFromStack(target, target.getMainHandItem(), EquipmentSlot.MAINHAND.getName(), attacker);
            applyOnHurtFromStack(target, target.getOffhandItem(), EquipmentSlot.OFFHAND.getName(), attacker);

            for (ItemStack armorStack : target.getArmorSlots()) {
                EquipmentSlot armorSlot = LivingEntity.getEquipmentSlotForItem(armorStack);
                String slotName = armorSlot != null ? armorSlot.getName() : "any";
                applyOnHurtFromStack(target, armorStack, slotName, attacker);
            }

            if (ModList.get().isLoaded("curios")) {
                applyCurioHurtEffects(target, attacker);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event == null) {
            return;
        }

        ItemStack stack = event.getItem();
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }

        LivingEntity holder = event.getEntity();
        if (holder == null || holder.level().isClientSide()) {
            return;
        }

        if (stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
            return;
        }

        if (stack.getItem() == Items.POTION && isOnUsePotionMirror(stack)) {
            return;
        }

        if (!isOnUseSupportedItem(stack)) {
            return;
        }

        processEffects(stack, ON_USE, holder, null, null);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ThrownPotion potion) {
            ItemStack stack = potion.getItem();
            if (!stack.isEmpty() && stack.hasTag()) {
                if (stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
                    if (!isOnUsePotionMirror(stack)) {
                        injectOnUseIntoPotionStack(stack);
                    }
                }
            }
        }
    }

    private static void injectOnUseIntoPotionStack(ItemStack potionStack) {
        CompoundTag tag = potionStack.getOrCreateTag();
        if (tag.getBoolean("SF_OnUseInjected")) {
            return;
        }

        CompoundTag sfTag = tag.getCompound(MAIN_TAG);
        if (sfTag.contains(ON_USE_COLOR, Tag.TAG_ANY_NUMERIC)) {
            tag.putInt(ItemEditorCompat.POTION_COLOR_TAG, sfTag.getInt(ON_USE_COLOR));
        }

        List<MobEffectInstance> onUseEffects = collectEffects(potionStack, ON_USE);
        if (onUseEffects.isEmpty()) {
            return;
        }

        ListTag customPotionEffects = tag.getList(ItemEditorCompat.POTION_EFFECTS_TAG, Tag.TAG_COMPOUND);
        for (MobEffectInstance effect : onUseEffects) {
            customPotionEffects.add(effect.save(new CompoundTag()));
        }
        tag.put(ItemEditorCompat.POTION_EFFECTS_TAG, customPotionEffects);
        tag.putBoolean("SF_OnUseInjected", true);
    }

    private static void processEffects(ItemStack stack, String typeTag, LivingEntity holder, LivingEntity other, String currentSlot) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MAIN_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag mainTag = tag.getCompound(MAIN_TAG);
        if (!mainTag.contains(typeTag, Tag.TAG_LIST)) {
            return;
        }

        ListTag effectList = mainTag.getList(typeTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < effectList.size(); i++) {
            CompoundTag entry = effectList.getCompound(i);

            if (!ON_USE.equals(typeTag)) {
                double chance = entry.contains("chance", Tag.TAG_ANY_NUMERIC) ? entry.getDouble("chance") : 1.0D;
                if (RANDOM.nextDouble() > chance) {
                    continue;
                }
            }

            if ((ON_HURT.equals(typeTag) || ON_EQUIP.equals(typeTag))) {
                String configuredSlot = entry.contains("Slot", Tag.TAG_STRING) ? entry.getString("Slot") : "any";
                if (!slotMatches(configuredSlot, currentSlot)) {
                    continue;
                }
            }

            MobEffectInstance effectInstance = createEffectInstance(entry);
            if (effectInstance == null) {
                continue;
            }

            LivingEntity applierTarget;
            if (ON_USE.equals(typeTag)) {
                applierTarget = other != null ? other : holder;
            } else {
                boolean targetSelf = entry.contains("self", Tag.TAG_BYTE) && entry.getBoolean("self");
                applierTarget = targetSelf ? holder : other;
            }

            if (applierTarget != null) {
                applierTarget.addEffect(new MobEffectInstance(effectInstance));
            }
        }
    }

    private static void applyOnHurtFromStack(LivingEntity target, ItemStack stack, String currentSlot, LivingEntity attacker) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        processEffects(stack, ON_HURT, target, attacker, currentSlot);
    }

    private static void applyOnEquipFromStack(LivingEntity wearer, ItemStack stack, String currentSlot) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MAIN_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag mainTag = tag.getCompound(MAIN_TAG);
        if (!mainTag.contains(ON_EQUIP, Tag.TAG_LIST)) {
            return;
        }

        ListTag effectList = mainTag.getList(ON_EQUIP, Tag.TAG_COMPOUND);
        for (int i = 0; i < effectList.size(); i++) {
            CompoundTag entry = effectList.getCompound(i);
            String targetSlot = entry.contains("Slot", Tag.TAG_STRING) ? entry.getString("Slot") : "any";
            if (!slotMatches(targetSlot, currentSlot)) {
                continue;
            }

            MobEffect effect = resolveEffect(entry);
            if (effect == null) {
                continue;
            }

            int amplifier = entry.getInt("amplifier");
            boolean ambient = entry.contains("Ambient", Tag.TAG_BYTE) && entry.getBoolean("Ambient");
            boolean showParticles = !entry.contains("ShowParticles", Tag.TAG_BYTE) || entry.getBoolean("ShowParticles");
            boolean showIcon = !entry.contains("ShowIcon", Tag.TAG_BYTE) || entry.getBoolean("ShowIcon");

            MobEffectInstance current = wearer.getEffect(effect);
            if (current != null && current.getAmplifier() == amplifier && current.getDuration() > (ON_EQUIP_APPLY_DURATION - ON_EQUIP_REFRESH_INTERVAL - 5)) {
                continue;
            }

            wearer.addEffect(new MobEffectInstance(effect, ON_EQUIP_APPLY_DURATION, amplifier, ambient, showParticles, showIcon));
        }
    }

    private static boolean slotMatches(String configuredSlot, String currentSlot) {
        String configured = normalizeSlotId(configuredSlot);
        String current = normalizeSlotId(currentSlot);
        if (configured.isBlank()) {
            return true;
        }
        if ("any".equalsIgnoreCase(configured)) {
            return true;
        }
        if (configured.equalsIgnoreCase(current)) {
            return true;
        }
        return configured.endsWith(":" + current) || current.endsWith(":" + configured);
    }

    private static String normalizeSlotId(String slot) {
        if (slot == null) {
            return "";
        }
        return slot.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static List<MobEffectInstance> collectEffects(ItemStack stack, String typeTag) {
        List<MobEffectInstance> effects = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MAIN_TAG, Tag.TAG_COMPOUND)) {
            return effects;
        }

        CompoundTag mainTag = tag.getCompound(MAIN_TAG);
        if (!mainTag.contains(typeTag, Tag.TAG_LIST)) {
            return effects;
        }

        ListTag effectList = mainTag.getList(typeTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < effectList.size(); i++) {
            CompoundTag entry = effectList.getCompound(i);

            if (!ON_USE.equals(typeTag)) {
                double chance = entry.contains("chance", Tag.TAG_ANY_NUMERIC) ? entry.getDouble("chance") : 1.0D;
                if (RANDOM.nextDouble() > chance) {
                    continue;
                }
            }

            MobEffectInstance effectInstance = createEffectInstance(entry);
            if (effectInstance != null) {
                effects.add(effectInstance);
            }
        }

        return effects;
    }

    private static MobEffectInstance createEffectInstance(CompoundTag entry) {
        MobEffect effect = resolveEffect(entry);
        if (effect == null) {
            return null;
        }

        int duration = entry.contains("duration", Tag.TAG_ANY_NUMERIC) ? entry.getInt("duration") : 0;
        int amplifier = entry.contains("amplifier", Tag.TAG_ANY_NUMERIC) ? entry.getInt("amplifier") : 0;
        boolean ambient = entry.contains("Ambient", Tag.TAG_BYTE) && entry.getBoolean("Ambient");
        boolean showParticles = !entry.contains("ShowParticles", Tag.TAG_BYTE) || entry.getBoolean("ShowParticles");
        boolean showIcon = !entry.contains("ShowIcon", Tag.TAG_BYTE) || entry.getBoolean("ShowIcon");

        return new MobEffectInstance(effect, duration, amplifier, ambient, showParticles, showIcon);
    }

    private static MobEffect resolveEffect(CompoundTag entry) {
        String key;
        if (entry.contains("id", Tag.TAG_STRING)) {
            key = "id:" + entry.getString("id");
            MobEffect cached = EFFECT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            ResourceLocation loc = ResourceLocation.tryParse(entry.getString("id"));
            if (loc != null) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(loc);
                if (effect != null) {
                    EFFECT_CACHE.put(key, effect);
                    return effect;
                }
            }
        }

        if (entry.contains("IdString", Tag.TAG_STRING)) {
            key = "idstr:" + entry.getString("IdString");
            MobEffect cached = EFFECT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            ResourceLocation loc = ResourceLocation.tryParse(entry.getString("IdString"));
            if (loc != null) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(loc);
                if (effect != null) {
                    EFFECT_CACHE.put(key, effect);
                    return effect;
                }
            }
        }

        if (entry.contains("Id", Tag.TAG_STRING)) {
            key = "id_alt:" + entry.getString("Id");
            MobEffect cached = EFFECT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            ResourceLocation loc = ResourceLocation.tryParse(entry.getString("Id"));
            if (loc != null) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(loc);
                if (effect != null) {
                    EFFECT_CACHE.put(key, effect);
                    return effect;
                }
            }
        }

        if (entry.contains("Id", Tag.TAG_ANY_NUMERIC)) {
            key = "num:" + entry.getInt("Id");
            MobEffect cached = EFFECT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.byId(entry.getInt("Id"));
            if (effect != null) {
                EFFECT_CACHE.put(key, effect);
            }
            return effect;
        }

        return null;
    }

    private static void applyCurioHurtEffects(LivingEntity target, LivingEntity attacker) {
        BabelCuriosSupport.forEachEquippedCurioStack(target, (slotId, index, curioStack) ->
                applyOnHurtFromStack(target, curioStack, slotId, attacker));
    }

    private static void applyCurioEquipEffects(LivingEntity wearer) {
        BabelCuriosSupport.forEachEquippedCurioStack(wearer, (slotId, index, curioStack) ->
                applyOnEquipFromStack(wearer, curioStack, slotId));
    }

    private static boolean isOnUseSupportedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
            return true;
        }

        UseAnim anim = stack.getUseAnimation();
        return stack.isEdible() || anim == UseAnim.EAT || anim == UseAnim.DRINK;
    }

    private static boolean isOnUsePotionMirror(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MAIN_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag sfTag = tag.getCompound(MAIN_TAG);
        return sfTag.getBoolean(ON_USE_MIRROR_TAG);
    }
}
