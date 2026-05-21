package com.gabri.itemeditor.editor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ItemEditorCompat {
    public static final int TICKS_PER_SECOND = 20;
    public static final int SPAWN_EGG_BABY_AGE = -24000;
    public static final String ATTRIBUTES_TAG = "AttributeModifiers";
    public static final String CURIOS_ATTRIBUTES_TAG = "CurioAttributeModifiers";
    public static final String ATTRIBUTES_BACKUP_TAG = "SF_BackupAttributes";
    public static final String EFFECTS_TAG = "SF_ItemEffects";
    public static final String EFFECTS_BACKUP_TAG = "SF_BackupEffects";
    public static final String POTION_EFFECTS_TAG = "CustomPotionEffects";
    public static final String POTION_COLOR_TAG = "CustomPotionColor";
    public static final String POTION_BASE_TAG = "Potion";
    public static final String POTION_BACKUP_TAG = "IE_BackupPotion";
    public static final String ENCHANTMENTS_TAG = "Enchantments";
    public static final String STORED_ENCHANTMENTS_TAG = "StoredEnchantments";

    private ItemEditorCompat() {
    }

    public enum EffectCategory {
        POTION("Potion", null),
        ON_USE("On Use", "on_use"),
        ON_HIT("On Hit", "on_hit"),
        ON_HURT("On Hurt", "on_hurt"),
        ON_EQUIP("On Equip", "on_equip");

        private final String label;
        private final String tagKey;

        EffectCategory(String label, String tagKey) {
            this.label = label;
            this.tagKey = tagKey;
        }

        public String label() {
            return label;
        }

        public String tagKey() {
            return tagKey;
        }

        public boolean isPotion() {
            return this == POTION;
        }
    }

    public record AttributeEntry(
            String attributeName,
            double amount,
            int operation,
            String slot,
            boolean curio,
            UUID uuid,
            int index
    ) {
    }

    public record EffectEntry(
            EffectCategory category,
            String id,
            int duration,
            int amplifier,
            double chance,
            boolean self,
            String slot,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            Integer color,
            int index
    ) {
    }

    public record EnchantmentEntry(String id, int level, int index) {
    }

    public record SpawnEggAttributeEntry(String attributeName, double base, int index) {
    }

    public record SpawnEggEffectEntry(
            String id,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            int index
    ) {
    }

    public static ItemStack sanitizeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        clampStackValues(copy, Math.max(1, copy.getMaxStackSize()));
        return copy;
    }

    public static CompoundTag fullNbt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }

        ItemStack copy = stack.copy();
        CompoundTag full = new CompoundTag();
        copy.save(full);
        return full;
    }

    public static ItemStack fromFullNbt(CompoundTag fullNbt) {
        if (fullNbt == null || fullNbt.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = ItemStack.of(fullNbt.copy());
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        clampStackValues(stack, Math.max(1, stack.getMaxStackSize()));
        return stack;
    }

    public static ResourceLocation itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ResourceLocation.tryParse("minecraft:air");
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null ? id : ResourceLocation.tryParse("minecraft:air");
    }

    public static int potionColor(CompoundTag tag) {
        return tag != null && tag.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC) ? tag.getInt(POTION_COLOR_TAG) : -1;
    }

    public static void setPotionColor(CompoundTag tag, Integer color) {
        if (tag == null) {
            return;
        }
        if (color == null) {
            tag.remove(POTION_COLOR_TAG);
        } else {
            tag.putInt(POTION_COLOR_TAG, color);
        }
    }

    public static String potionBase(CompoundTag tag) {
        if (tag == null || !tag.contains(POTION_BASE_TAG, Tag.TAG_STRING)) {
            return "";
        }
        return tag.getString(POTION_BASE_TAG);
    }

    public static void setPotionBase(CompoundTag tag, String potionId) {
        if (tag == null) {
            return;
        }
        if (potionId == null || potionId.isBlank()) {
            tag.remove(POTION_BASE_TAG);
        } else {
            tag.putString(POTION_BASE_TAG, normalizeId(potionId));
        }
    }

    public static boolean isSpawnEgg(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SpawnEggItem;
    }

    public static CompoundTag spawnEggEntityTag(CompoundTag itemTag) {
        if (itemTag == null || !itemTag.contains("EntityTag", Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return itemTag.getCompound("EntityTag").copy();
    }

    public static void setSpawnEggEntityTag(CompoundTag itemTag, CompoundTag entityTag) {
        if (itemTag == null) {
            return;
        }
        if (entityTag == null || entityTag.isEmpty()) {
            itemTag.remove("EntityTag");
        } else {
            itemTag.put("EntityTag", entityTag);
        }
    }

    public static boolean isSpawnEggBaby(CompoundTag entityTag) {
        if (entityTag == null) {
            return false;
        }
        if (entityTag.contains("Age", Tag.TAG_ANY_NUMERIC) && entityTag.getInt("Age") < 0) {
            return true;
        }
        return entityTag.contains("IsBaby", Tag.TAG_BYTE) && entityTag.getBoolean("IsBaby");
    }

    public static void setSpawnEggBaby(CompoundTag entityTag, boolean baby) {
        if (entityTag == null) {
            return;
        }
        if (baby) {
            entityTag.putInt("Age", SPAWN_EGG_BABY_AGE);
            entityTag.putBoolean("IsBaby", true);
            return;
        }
        entityTag.remove("Age");
        entityTag.remove("IsBaby");
    }

    public static boolean isSpawnEggFlagEnabled(CompoundTag entityTag, String key) {
        return entityTag != null
                && key != null
                && entityTag.contains(key, Tag.TAG_BYTE)
                && entityTag.getBoolean(key);
    }

    public static void setSpawnEggFlag(CompoundTag entityTag, String key, boolean enabled) {
        if (entityTag == null || key == null || key.isBlank()) {
            return;
        }
        if (enabled) {
            entityTag.putBoolean(key, true);
        } else {
            entityTag.remove(key);
        }
    }

    public static String spawnEggDefaultLootTable(ItemStack stack, CompoundTag itemTag) {
        if (!isSpawnEgg(stack)) {
            return "minecraft:empty";
        }
        try {
            SpawnEggItem eggItem = (SpawnEggItem) stack.getItem();
            var entityType = eggItem.getType(itemTag == null ? new CompoundTag() : itemTag);
            ResourceLocation lootTable = entityType.getDefaultLootTable();
            return lootTable != null ? lootTable.toString() : "minecraft:empty";
        } catch (Exception ignored) {
            return "minecraft:empty";
        }
    }

    public static List<SpawnEggAttributeEntry> readSpawnEggAttributes(CompoundTag entityTag) {
        List<SpawnEggAttributeEntry> result = new ArrayList<>();
        if (entityTag == null || !entityTag.contains("Attributes", Tag.TAG_LIST)) {
            return result;
        }

        ListTag list = entityTag.getList("Attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String attributeName = entry.contains("Name", Tag.TAG_STRING)
                    ? normalizeId(entry.getString("Name"))
                    : entry.contains("AttributeName", Tag.TAG_STRING) ? normalizeId(entry.getString("AttributeName")) : "";
            double base = entry.contains("Base", Tag.TAG_ANY_NUMERIC) ? entry.getDouble("Base") : 0.0D;
            result.add(new SpawnEggAttributeEntry(attributeName, base, i));
        }
        return result;
    }

    public static void writeSpawnEggAttributes(CompoundTag entityTag, List<SpawnEggAttributeEntry> entries) {
        if (entityTag == null) {
            return;
        }

        ListTag list = new ListTag();
        if (entries != null) {
            for (SpawnEggAttributeEntry entry : entries) {
                CompoundTag compound = new CompoundTag();
                compound.putString("Name", normalizeId(entry.attributeName()));
                compound.putDouble("Base", entry.base());
                list.add(compound);
            }
        }

        putListOrRemove(entityTag, "Attributes", list);
    }

    public static List<SpawnEggEffectEntry> readSpawnEggEffects(CompoundTag entityTag) {
        List<SpawnEggEffectEntry> result = new ArrayList<>();
        if (entityTag == null || !entityTag.contains("ActiveEffects", Tag.TAG_LIST)) {
            return result;
        }

        ListTag list = entityTag.getList("ActiveEffects", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String id = readPotionEffectId(entry);
            int duration = entry.contains("Duration", Tag.TAG_ANY_NUMERIC) ? ticksToSeconds(entry.getInt("Duration")) : 0;
            int amplifier = entry.contains("Amplifier", Tag.TAG_ANY_NUMERIC) ? entry.getInt("Amplifier") : 0;
            boolean ambient = entry.contains("Ambient", Tag.TAG_BYTE) && entry.getBoolean("Ambient");
            boolean showParticles = !entry.contains("ShowParticles", Tag.TAG_BYTE) || entry.getBoolean("ShowParticles");
            boolean showIcon = !entry.contains("ShowIcon", Tag.TAG_BYTE) || entry.getBoolean("ShowIcon");
            result.add(new SpawnEggEffectEntry(id, duration, amplifier, ambient, showParticles, showIcon, i));
        }
        return result;
    }

    public static void writeSpawnEggEffects(CompoundTag entityTag, List<SpawnEggEffectEntry> entries) {
        if (entityTag == null) {
            return;
        }

        ListTag list = new ListTag();
        if (entries != null) {
            for (SpawnEggEffectEntry entry : entries) {
                CompoundTag compound = new CompoundTag();
                writePotionEffectId(compound, entry.id());
                compound.putInt("Duration", secondsToTicks(entry.duration()));
                compound.putInt("Amplifier", Math.max(0, entry.amplifier()));
                compound.putBoolean("Ambient", entry.ambient());
                compound.putBoolean("ShowParticles", entry.showParticles());
                compound.putBoolean("ShowIcon", entry.showIcon());
                list.add(compound);
            }
        }

        putListOrRemove(entityTag, "ActiveEffects", list);
    }

    public static List<AttributeEntry> readAttributes(CompoundTag tag) {
        List<AttributeEntry> result = new ArrayList<>();
        if (tag == null) {
            return result;
        }

        boolean readMainVanilla = false;
        if (tag.contains(ATTRIBUTES_TAG, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ATTRIBUTES_TAG, Tag.TAG_COMPOUND);
            if (!list.isEmpty()) {
                for (int i = 0; i < list.size(); i++) {
                    result.add(readAttributeEntry(list.getCompound(i), false, i));
                }
                readMainVanilla = true;
            }
        }

        boolean readMainCurio = false;
        if (tag.contains(CURIOS_ATTRIBUTES_TAG, Tag.TAG_LIST)) {
            ListTag list = tag.getList(CURIOS_ATTRIBUTES_TAG, Tag.TAG_COMPOUND);
            if (!list.isEmpty()) {
                for (int i = 0; i < list.size(); i++) {
                    result.add(readAttributeEntry(list.getCompound(i), true, i));
                }
                readMainCurio = true;
            }
        }

        if ((!readMainVanilla || !readMainCurio) && tag.contains(ATTRIBUTES_BACKUP_TAG, Tag.TAG_LIST)) {
            ListTag backup = tag.getList(ATTRIBUTES_BACKUP_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < backup.size(); i++) {
                CompoundTag entry = backup.getCompound(i);
                boolean curio = entry.contains("IsCurio", Tag.TAG_BYTE) && entry.getBoolean("IsCurio");
                if (curio && readMainCurio) {
                    continue;
                }
                if (!curio && readMainVanilla) {
                    continue;
                }
                result.add(readAttributeEntry(entry, curio, i));
            }
        }

        return result;
    }

    public static void writeAttributes(CompoundTag tag, List<AttributeEntry> entries) {
        if (tag == null) {
            return;
        }

        ListTag vanilla = new ListTag();
        ListTag curios = new ListTag();
        if (entries != null) {
            for (AttributeEntry entry : entries) {
                CompoundTag compound = writeAttributeEntry(entry);
                if (entry.curio()) {
                    curios.add(compound);
                } else {
                    vanilla.add(compound);
                }
            }
        }

        putListOrRemove(tag, ATTRIBUTES_TAG, vanilla);
        putListOrRemove(tag, CURIOS_ATTRIBUTES_TAG, curios);
        syncAttributeBackup(tag);
    }

    public static List<EffectEntry> readEffects(CompoundTag tag, EffectCategory category) {
        List<EffectEntry> result = new ArrayList<>();
        if (tag == null || category == null) {
            return result;
        }

        if (category.isPotion()) {
            Integer color = potionColor(tag);
            if (tag.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST)) {
                ListTag list = tag.getList(POTION_EFFECTS_TAG, Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    result.add(readPotionEffectEntry(list.getCompound(i), i, color));
                }
            }
            if (tag.contains(POTION_BASE_TAG, Tag.TAG_STRING)) {
                ResourceLocation potionId = ResourceLocation.tryParse(tag.getString(POTION_BASE_TAG));
                if (potionId != null) {
                    Potion potion = ForgeRegistries.POTIONS.getValue(potionId);
                    if (potion != null) {
                        int baseIndex = -1;
                        for (var effectInstance : potion.getEffects()) {
                            CompoundTag synthetic = new CompoundTag();
                            ResourceLocation effectKey = ForgeRegistries.MOB_EFFECTS.getKey(effectInstance.getEffect());
                            if (effectKey != null) {
                            synthetic.putString("IdString", effectKey.toString());
                            } else {
                                synthetic.putInt("Id", BuiltInRegistries.MOB_EFFECT.getId(effectInstance.getEffect()));
                            }
                            synthetic.putString("id", effectKey != null ? effectKey.toString() : "");
                            synthetic.putByte("amplifier", (byte) effectInstance.getAmplifier());
                            synthetic.putBoolean("ambient", effectInstance.isAmbient());
                            synthetic.putBoolean("show_particles", effectInstance.isVisible());
                            synthetic.putBoolean("show_icon", effectInstance.showIcon());
                            synthetic.putInt("Duration", effectInstance.getDuration());
                            synthetic.putInt("Amplifier", effectInstance.getAmplifier());
                            synthetic.putBoolean("Ambient", effectInstance.isAmbient());
                            synthetic.putBoolean("ShowParticles", effectInstance.isVisible());
                            synthetic.putBoolean("ShowIcon", effectInstance.showIcon());
                            result.add(readPotionEffectEntry(synthetic, baseIndex--, color));
                        }
                    }
                }
            }
            return result;
        }

        ListTag list = null;
        if (tag.contains(EFFECTS_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag main = tag.getCompound(EFFECTS_TAG);
            if (main.contains(category.tagKey(), Tag.TAG_LIST)) {
                ListTag mainList = main.getList(category.tagKey(), Tag.TAG_COMPOUND);
                if (!mainList.isEmpty()) {
                    list = mainList;
                }
            }
        }
        if ((list == null || list.isEmpty()) && tag.contains(EFFECTS_BACKUP_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag backup = tag.getCompound(EFFECTS_BACKUP_TAG);
            if (backup.contains(category.tagKey(), Tag.TAG_LIST)) {
                ListTag backupList = backup.getList(category.tagKey(), Tag.TAG_COMPOUND);
                if (!backupList.isEmpty()) {
                    list = backupList;
                }
            }
        }
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                result.add(readLegacyEffectEntry(list.getCompound(i), category, i));
            }
        }
        return result;
    }

    public static void writeEffects(CompoundTag tag, EffectCategory category, List<EffectEntry> entries) {
        if (tag == null || category == null) {
            return;
        }

        if (category.isPotion()) {
            ListTag list = new ListTag();
            if (entries != null) {
                for (EffectEntry entry : entries) {
                    if (entry.index() >= 0) {
                        list.add(writePotionEffectEntry(entry));
                    }
                }
            }
            putListOrRemove(tag, POTION_EFFECTS_TAG, list);
            return;
        }

        CompoundTag main = tag.contains(EFFECTS_TAG, Tag.TAG_COMPOUND) ? tag.getCompound(EFFECTS_TAG).copy() : new CompoundTag();
        ListTag list = new ListTag();
        if (entries != null) {
            for (EffectEntry entry : entries) {
                list.add(writeLegacyEffectEntry(entry));
            }
        }
        if (list.isEmpty()) {
            main.remove(category.tagKey());
        } else {
            main.put(category.tagKey(), list);
        }
        if (main.isEmpty()) {
            tag.remove(EFFECTS_TAG);
        } else {
            tag.put(EFFECTS_TAG, main);
        }
        syncEffectBackup(tag);
    }

    public static List<EnchantmentEntry> readEnchantments(ItemStack stack, CompoundTag tag) {
        List<EnchantmentEntry> result = new ArrayList<>();
        if (tag == null) {
            return result;
        }

        String storageTag = enchantmentStorageTag(stack);
        String fallbackTag = alternateEnchantmentStorageTag(storageTag);
        ListTag list = readEnchantmentList(tag, storageTag);
        if ((list == null || list.isEmpty()) && fallbackTag != null) {
            list = readEnchantmentList(tag, fallbackTag);
        }

        if (list == null) {
            return result;
        }

        for (int i = 0; i < list.size(); i++) {
            result.add(readEnchantmentEntry(list.getCompound(i), i));
        }
        return result;
    }

    public static void writeEnchantments(ItemStack stack, CompoundTag tag, List<EnchantmentEntry> entries) {
        if (tag == null) {
            return;
        }

        String storageTag = enchantmentStorageTag(stack);
        String fallbackTag = alternateEnchantmentStorageTag(storageTag);

        ListTag list = new ListTag();
        if (entries != null) {
            for (EnchantmentEntry entry : entries) {
                list.add(writeEnchantmentEntry(entry));
            }
        }

        putListOrRemove(tag, storageTag, list);
        if (fallbackTag != null) {
            tag.remove(fallbackTag);
        }
    }

    public static void syncEffectBackups(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        syncPersistentBackups(tag);
    }

    public static void syncPersistentBackups(CompoundTag tag) {
        syncPersistentBackups(null, tag);
    }

    public static void syncPersistentBackups(ItemStack stack, CompoundTag tag) {
        if (tag == null) {
            return;
        }
        boolean potionStack = stack == null || isPotionItem(stack);
        if (stack != null && isPotionItem(stack)) {
            mergePotionPersistence(stack.getTag(), tag);
        }
        restoreAttributeBackup(tag);
        restoreEffectBackup(tag);
        if (potionStack) {
            restorePotionBackup(tag);
        }
        syncAttributeBackup(tag);
        syncEffectBackup(tag);
        if (potionStack) {
            syncPotionBackup(tag);
        }
    }

    public static List<String> vanillaSlotOptions() {
        return List.of("any", "mainhand", "offhand", "head", "chest", "legs", "feet");
    }

    public static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static AttributeEntry readAttributeEntry(CompoundTag entry, boolean curio, int index) {
        String attributeName = entry.contains("AttributeName", Tag.TAG_STRING) ? normalizeId(entry.getString("AttributeName")) : "";
        double amount = entry.contains("Amount", Tag.TAG_ANY_NUMERIC) ? entry.getDouble("Amount") : 0.0D;
        int operation = entry.contains("Operation", Tag.TAG_ANY_NUMERIC) ? entry.getInt("Operation") : AttributeModifier.Operation.ADDITION.ordinal();
        String slot = entry.contains("Slot", Tag.TAG_STRING) ? entry.getString("Slot") : "any";
        UUID uuid = entry.hasUUID("UUID") ? entry.getUUID("UUID") : UUID.randomUUID();
        return new AttributeEntry(attributeName, amount, operation, slot, curio, uuid, index);
    }

    private static CompoundTag writeAttributeEntry(AttributeEntry entry) {
        CompoundTag compound = new CompoundTag();
        compound.putString("AttributeName", normalizeId(entry.attributeName()));
        compound.putString("Name", normalizeId(entry.attributeName()));
        compound.putDouble("Amount", entry.amount());
        compound.putInt("Operation", Math.max(0, Math.min(2, entry.operation())));
        compound.putUUID("UUID", entry.uuid() != null ? entry.uuid() : UUID.randomUUID());
        if (entry.slot() != null && !entry.slot().isBlank()) {
            compound.putString("Slot", entry.slot());
        }
        return compound;
    }

    private static EffectEntry readPotionEffectEntry(CompoundTag entry, int index, Integer color) {
        String id = readPotionEffectId(entry);
        int duration = entry.contains("duration", Tag.TAG_ANY_NUMERIC)
                ? ticksToSeconds(entry.getInt("duration"))
                : entry.contains("Duration", Tag.TAG_ANY_NUMERIC) ? ticksToSeconds(entry.getInt("Duration")) : 0;
        int amplifier = entry.contains("amplifier", Tag.TAG_ANY_NUMERIC)
                ? entry.getByte("amplifier")
                : entry.contains("Amplifier", Tag.TAG_ANY_NUMERIC) ? entry.getInt("Amplifier") : 0;
        boolean ambient = entry.contains("ambient", Tag.TAG_BYTE)
                ? entry.getBoolean("ambient")
                : entry.contains("Ambient", Tag.TAG_BYTE) && entry.getBoolean("Ambient");
        boolean showParticles = entry.contains("show_particles", Tag.TAG_BYTE)
                ? entry.getBoolean("show_particles")
                : !entry.contains("ShowParticles", Tag.TAG_BYTE) || entry.getBoolean("ShowParticles");
        boolean showIcon = entry.contains("show_icon", Tag.TAG_BYTE)
                ? entry.getBoolean("show_icon")
                : !entry.contains("ShowIcon", Tag.TAG_BYTE) || entry.getBoolean("ShowIcon");
        return new EffectEntry(EffectCategory.POTION, id, duration, amplifier, 1.0D, false, "", ambient, showParticles, showIcon, color, index);
    }

    private static CompoundTag writePotionEffectEntry(EffectEntry entry) {
        CompoundTag compound = new CompoundTag();
        writePotionEffectId(compound, entry.id());
        compound.putString("id", normalizeId(entry.id()));
        compound.putByte("amplifier", (byte) Math.max(0, entry.amplifier()));
        compound.putInt("duration", secondsToTicks(entry.duration()));
        compound.putBoolean("ambient", entry.ambient());
        compound.putBoolean("show_particles", entry.showParticles());
        compound.putBoolean("show_icon", entry.showIcon());
        compound.putInt("Duration", secondsToTicks(entry.duration()));
        compound.putInt("Amplifier", Math.max(0, entry.amplifier()));
        compound.putBoolean("Ambient", entry.ambient());
        compound.putBoolean("ShowParticles", entry.showParticles());
        compound.putBoolean("ShowIcon", entry.showIcon());
        return compound;
    }

    private static EffectEntry readLegacyEffectEntry(CompoundTag entry, EffectCategory category, int index) {
        String id = normalizeId(entry.contains("id", Tag.TAG_STRING) ? entry.getString("id") : entry.contains("IdString", Tag.TAG_STRING) ? entry.getString("IdString") : "");
        int duration = entry.contains("duration", Tag.TAG_ANY_NUMERIC) ? ticksToSeconds(entry.getInt("duration")) : entry.contains("Duration", Tag.TAG_ANY_NUMERIC) ? ticksToSeconds(entry.getInt("Duration")) : 0;
        int amplifier = entry.contains("amplifier", Tag.TAG_ANY_NUMERIC) ? entry.getInt("amplifier") : entry.contains("Amplifier", Tag.TAG_ANY_NUMERIC) ? entry.getInt("Amplifier") : 0;
        double chance = entry.contains("chance", Tag.TAG_ANY_NUMERIC) ? entry.getDouble("chance") : 1.0D;
        boolean self = entry.contains("self", Tag.TAG_BYTE) && entry.getBoolean("self");
        String slot = entry.contains("Slot", Tag.TAG_STRING) ? entry.getString("Slot") : "any";
        boolean ambient = entry.contains("Ambient", Tag.TAG_BYTE) && entry.getBoolean("Ambient");
        boolean showParticles = !entry.contains("ShowParticles", Tag.TAG_BYTE) || entry.getBoolean("ShowParticles");
        boolean showIcon = !entry.contains("ShowIcon", Tag.TAG_BYTE) || entry.getBoolean("ShowIcon");
        Integer color = entry.contains("Color", Tag.TAG_ANY_NUMERIC) ? entry.getInt("Color") : null;
        return new EffectEntry(category, id, duration, amplifier, chance, self, slot, ambient, showParticles, showIcon, color, index);
    }

    private static CompoundTag writeLegacyEffectEntry(EffectEntry entry) {
        CompoundTag compound = new CompoundTag();
        compound.putString("id", normalizeId(entry.id()));
        compound.putInt("duration", secondsToTicks(entry.duration()));
        compound.putInt("amplifier", Math.max(0, entry.amplifier()));
        compound.putDouble("chance", entry.chance());
        compound.putBoolean("self", entry.self());
        if (entry.slot() != null && !entry.slot().isBlank()) {
            compound.putString("Slot", entry.slot());
        }
        compound.putBoolean("Ambient", entry.ambient());
        compound.putBoolean("ShowParticles", entry.showParticles());
        compound.putBoolean("ShowIcon", entry.showIcon());
        if (entry.color() != null) {
            compound.putInt("Color", entry.color());
        }
        return compound;
    }

    private static String readPotionEffectId(CompoundTag entry) {
        if (entry.contains("IdString", Tag.TAG_STRING)) {
            return normalizeId(entry.getString("IdString"));
        }
        if (entry.contains("id", Tag.TAG_STRING)) {
            return normalizeId(entry.getString("id"));
        }
        if (entry.contains("Id", Tag.TAG_STRING)) {
            return normalizeId(entry.getString("Id"));
        }
        if (entry.contains("Id", Tag.TAG_ANY_NUMERIC)) {
            int raw = entry.getInt("Id");
            var effect = BuiltInRegistries.MOB_EFFECT.byId(raw);
            if (effect != null) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                if (id != null) {
                    return id.toString();
                }
            }
        }
        return "";
    }

    private static void writePotionEffectId(CompoundTag entry, String normalizedId) {
        ResourceLocation effectRl = ResourceLocation.tryParse(normalizedId);
        if (effectRl != null) {
            var effect = ForgeRegistries.MOB_EFFECTS.getValue(effectRl);
            if (effect != null) {
                int rawId = BuiltInRegistries.MOB_EFFECT.getId(effect);
                entry.putInt("Id", rawId);
                entry.putString("IdString", effectRl.toString());
                entry.putString("id", effectRl.toString());
                return;
            }
        }
        entry.putString("Id", normalizedId);
        entry.putString("id", normalizeId(normalizedId));
    }

    public static int ticksToSeconds(int ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public static int secondsToTicks(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return seconds * TICKS_PER_SECOND;
    }

    private static void putListOrRemove(CompoundTag tag, String key, ListTag list) {
        if (list == null || list.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, list);
        }
    }

    private static void syncAttributeBackup(CompoundTag tag) {
        ListTag backup = new ListTag();
        if (tag.contains(ATTRIBUTES_TAG, Tag.TAG_LIST)) {
            ListTag vanilla = tag.getList(ATTRIBUTES_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < vanilla.size(); i++) {
                CompoundTag copy = vanilla.getCompound(i).copy();
                backup.add(copy);
            }
        }
        putListOrRemove(tag, ATTRIBUTES_BACKUP_TAG, backup);
    }

    private static ListTag readEnchantmentList(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.contains(key, Tag.TAG_LIST)) {
            return null;
        }
        return tag.getList(key, Tag.TAG_COMPOUND);
    }

    private static EnchantmentEntry readEnchantmentEntry(CompoundTag entry, int index) {
        String id = entry.contains("id", Tag.TAG_STRING)
                ? normalizeId(entry.getString("id"))
                : entry.contains("IdString", Tag.TAG_STRING)
                ? normalizeId(entry.getString("IdString"))
                : "";
        int level = entry.contains("lvl", Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, entry.getShort("lvl"))
                : entry.contains("Level", Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, entry.getInt("Level"))
                : 1;
        return new EnchantmentEntry(id, level, index);
    }

    private static CompoundTag writeEnchantmentEntry(EnchantmentEntry entry) {
        CompoundTag compound = new CompoundTag();
        String id = normalizeId(entry.id());
        compound.putString("id", id);
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null) {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(rl);
            if (enchantment != null) {
                compound.putShort("lvl", (short) Math.max(1, entry.level()));
                return compound;
            }
        }
        compound.putShort("lvl", (short) Math.max(1, entry.level()));
        return compound;
    }

    public static boolean usesStoredEnchantments(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.ENCHANTED_BOOK);
    }

    public static boolean isPotionItem(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

    public static String enchantmentStorageTag(ItemStack stack) {
        return usesStoredEnchantments(stack) ? STORED_ENCHANTMENTS_TAG : ENCHANTMENTS_TAG;
    }

    public static String alternateEnchantmentStorageTag(String storageTag) {
        if (STORED_ENCHANTMENTS_TAG.equals(storageTag)) {
            return ENCHANTMENTS_TAG;
        }
        if (ENCHANTMENTS_TAG.equals(storageTag)) {
            return STORED_ENCHANTMENTS_TAG;
        }
        return null;
    }

    private static void restoreAttributeBackup(CompoundTag tag) {
        boolean vanillaMissing = !tag.contains(ATTRIBUTES_TAG, Tag.TAG_LIST) || tag.getList(ATTRIBUTES_TAG, Tag.TAG_COMPOUND).isEmpty();
        if (!vanillaMissing || !tag.contains(ATTRIBUTES_BACKUP_TAG, Tag.TAG_LIST)) {
            return;
        }

        ListTag backup = tag.getList(ATTRIBUTES_BACKUP_TAG, Tag.TAG_COMPOUND);
        ListTag vanilla = new ListTag();
        for (int i = 0; i < backup.size(); i++) {
            CompoundTag entry = backup.getCompound(i);
            boolean curio = entry.contains("IsCurio", Tag.TAG_BYTE) && entry.getBoolean("IsCurio");
            if (!curio) {
                vanilla.add(entry.copy());
            }
        }
        if (!vanilla.isEmpty()) {
            tag.put(ATTRIBUTES_TAG, vanilla);
        }
    }

    private static void syncEffectBackup(CompoundTag tag) {
        if (tag.contains(EFFECTS_TAG, Tag.TAG_COMPOUND)) {
            tag.put(EFFECTS_BACKUP_TAG, tag.getCompound(EFFECTS_TAG).copy());
        } else {
            tag.remove(EFFECTS_BACKUP_TAG);
        }
    }

    private static void restoreEffectBackup(CompoundTag tag) {
        boolean effectsMissing = !tag.contains(EFFECTS_TAG, Tag.TAG_COMPOUND) || tag.getCompound(EFFECTS_TAG).isEmpty();
        if (!effectsMissing || !tag.contains(EFFECTS_BACKUP_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        tag.put(EFFECTS_TAG, tag.getCompound(EFFECTS_BACKUP_TAG).copy());
    }

    private static void syncPotionBackup(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        if (!hasPotionData(tag)) {
            tag.remove(POTION_BACKUP_TAG);
            return;
        }

        CompoundTag backup = new CompoundTag();
        if (tag.contains(POTION_BASE_TAG, Tag.TAG_STRING)) {
            backup.putString(POTION_BASE_TAG, tag.getString(POTION_BASE_TAG));
        }
        if (tag.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST)) {
            backup.put(POTION_EFFECTS_TAG, tag.getList(POTION_EFFECTS_TAG, Tag.TAG_COMPOUND).copy());
        }
        if (tag.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC)) {
            backup.putInt(POTION_COLOR_TAG, tag.getInt(POTION_COLOR_TAG));
        }
        putCompoundOrRemove(tag, POTION_BACKUP_TAG, backup);
    }

    private static void restorePotionBackup(CompoundTag tag) {
        if (tag == null || !tag.contains(POTION_BACKUP_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag backup = tag.getCompound(POTION_BACKUP_TAG);
        boolean baseMissing = !tag.contains(POTION_BASE_TAG, Tag.TAG_STRING);
        boolean effectsMissing = !tag.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST) || tag.getList(POTION_EFFECTS_TAG, Tag.TAG_COMPOUND).isEmpty();
        boolean colorMissing = !tag.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC);

        if (baseMissing && backup.contains(POTION_BASE_TAG, Tag.TAG_STRING)) {
            tag.putString(POTION_BASE_TAG, backup.getString(POTION_BASE_TAG));
        }
        if (effectsMissing && backup.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST)) {
            tag.put(POTION_EFFECTS_TAG, backup.getList(POTION_EFFECTS_TAG, Tag.TAG_COMPOUND).copy());
        }
        if (colorMissing && backup.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC)) {
            tag.putInt(POTION_COLOR_TAG, backup.getInt(POTION_COLOR_TAG));
        }
    }

    private static void mergePotionPersistence(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        if (!isPotionTagPresent(source)) {
            return;
        }

        if (!target.contains(POTION_BASE_TAG, Tag.TAG_STRING) && source.contains(POTION_BASE_TAG, Tag.TAG_STRING)) {
            target.putString(POTION_BASE_TAG, source.getString(POTION_BASE_TAG));
        }
        if ((!target.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST) || target.getList(POTION_EFFECTS_TAG, Tag.TAG_COMPOUND).isEmpty())
                && source.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST)) {
            target.put(POTION_EFFECTS_TAG, source.getList(POTION_EFFECTS_TAG, Tag.TAG_COMPOUND).copy());
        }
        if (!target.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC) && source.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC)) {
            target.putInt(POTION_COLOR_TAG, source.getInt(POTION_COLOR_TAG));
        }
        if (!target.contains(POTION_BACKUP_TAG, Tag.TAG_COMPOUND) && source.contains(POTION_BACKUP_TAG, Tag.TAG_COMPOUND)) {
            target.put(POTION_BACKUP_TAG, source.getCompound(POTION_BACKUP_TAG).copy());
        }
    }

    private static boolean isPotionTagPresent(CompoundTag tag) {
        return tag != null && (tag.contains(POTION_BASE_TAG, Tag.TAG_STRING)
                || tag.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST)
                || tag.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC)
                || tag.contains(POTION_BACKUP_TAG, Tag.TAG_COMPOUND));
    }

    private static boolean hasPotionData(CompoundTag tag) {
        return tag != null && (tag.contains(POTION_BASE_TAG, Tag.TAG_STRING)
                || tag.contains(POTION_EFFECTS_TAG, Tag.TAG_LIST)
                || tag.contains(POTION_COLOR_TAG, Tag.TAG_ANY_NUMERIC));
    }

    private static void clampStackValues(ItemStack stack, int maxCount) {
        int clampedCount = Math.max(1, Math.min(maxCount, stack.getCount()));
        stack.setCount(clampedCount);
        if (stack.isDamageableItem()) {
            int maxDamage = Math.max(0, stack.getMaxDamage());
            stack.setDamageValue(Math.max(0, Math.min(maxDamage, stack.getDamageValue())));
        }
    }

    private static void putCompoundOrRemove(CompoundTag tag, String key, CompoundTag value) {
        if (value == null || value.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, value);
        }
    }
}
