package me.chrisumb.entitymanager.util;

import me.chrisumb.entitymanager.module.drops.CustomDrop;
import me.chrisumb.entitymanager.module.stacking.Stacker;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static me.chrisumb.entitymanager.util.ReflectionUtil.*;

public final class EntityUtil {

    private static Method regenerateEquipmentEnchantsMethod = null;
    private static Method regenerateEquipmentMethod = null;
    private static Method sheepGenerateColorMethod = null;
    private static Method getCombatTrackerMethod = null;
    private static Method getDamageScalerMethod = null;
    private static Method getBukkitEntityMethod = null;
    private static Method dropDeathLootMethod = null;
    private static Method dropEquipmentMethod = null;
    private static Method sheepSetColorMethod = null;
    private static Method getExpRewardMethod = null;
    private static Method getRareDropMethod = null;
    private static Method getHandleMethod = null;
    private static Method addDropsMethod = null;

    private static Field genericDamageSourceField = null;
    private static Field combatEntryListField = null;
    private static Field damageSourceField = null;
    private static Field killerField = null;
    private static Field dropsField = null;
    private static Field worldField = null;

    private static Constructor<?> blockPositionConstructor = null;

    static {
        NMSVersion version = NMS_VERSION;

        try {
            Class<?> scalerClass = getVanillaClass("DifficultyDamageScaler");
            Class<?> blockPosition = getVanillaClass("BlockPosition");

            regenerateEquipmentEnchantsMethod = getMethodIfValid(false, "EntityInsentient", "b", scalerClass);
            regenerateEquipmentMethod = getMethodIfValid(false, "EntityInsentient", "a", scalerClass);
            sheepGenerateColorMethod = getMethodIfValid(false, "EntitySheep", "a", Random.class);
            getCombatTrackerMethod = getMethodIfValid(false, "EntityLiving", "getCombatTracker");
            getBukkitEntityMethod = getMethodIfValid(false, "Entity", "getBukkitEntity");
            dropDeathLootMethod = getMethodIfValid(false, "EntityLiving", "dropDeathLoot", boolean.class, int.class);
            dropEquipmentMethod = getMethodIfValid(false, "EntityLiving", "dropEquipment", boolean.class, int.class);
            sheepSetColorMethod = getMethodIfValid(false, "EntitySheep", "setColor", getVanillaClass("EnumColor"));
            getExpRewardMethod = getMethodIfValid(false, "EntityLiving", "getExpReward");
            getRareDropMethod = getMethodIfValid(false, "EntityLiving", "getRareDrop");
            getHandleMethod = getMethodIfValid(true, "entity.CraftLivingEntity", "getHandle");
            addDropsMethod = getMethodIfValid(false, "EntityLiving", "a", boolean.class, int.class, getVanillaClass("DamageSource"));

            genericDamageSourceField = getFieldIfValid(false, "DamageSource", "GENERIC");
            combatEntryListField = getFieldIfValid(false, "CombatTracker", "a");
            damageSourceField = getFieldIfValid(false, "CombatEntry", "a");
            killerField = getFieldIfValid(false, "EntityLiving", "killer");
            worldField = getFieldIfValid(false, "Entity", "world");
            dropsField = getFieldIfValid(false, "EntityLiving", "drops");

            //this.setColor(a(this.world.random));

            if (version.isBefore(NMSVersion.v1_9_R1)) {
                getDamageScalerMethod = getMethodIfValid(false, "World", "E", blockPosition);
            }

            if (version.isAfter(NMSVersion.v1_8_R3) && version.isBefore(NMSVersion.v1_13_R2)) {
                getDamageScalerMethod = getMethodIfValid(false, "World", "D", blockPosition);
            }

            if (version.isAfter(NMSVersion.v1_12_R1)) {
                getDamageScalerMethod = getMethodIfValid(false, "World", "getDamageScaler", blockPosition);
            }

            blockPositionConstructor = blockPosition.getConstructor(getVanillaClass("Entity"));
            blockPositionConstructor.setAccessible(true);

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Object getEntityHandle(LivingEntity entity) {
        try {
            return getHandleMethod.invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static int getExperienceDrops(LivingEntity entity, Player killer) {
        return getExperienceDrops(getEntityHandle(entity), killer == null ? null : getEntityHandle(killer));
    }

    //Reflection for getting experience drops.
    public static int getExperienceDrops(Object entityHandle, Object killerHandle) {
        try {
            killerField.set(entityHandle, killerHandle);

            return (int) getExpRewardMethod.invoke(entityHandle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static List<ItemStack> getItemDrops(LivingEntity entity) {
        return getItemDrops(getEntityHandle(entity), entity.getKiller(), null);
    }

    public static void regenerateEquipment(Object entityHandle) {
        try {
            Object craftEntity = getBukkitEntityMethod.invoke(entityHandle);
            LivingEntity livingEntity = (LivingEntity) craftEntity;
            if(livingEntity instanceof Sheep) {
                sheepSetColorMethod.invoke(entityHandle, sheepGenerateColorMethod.invoke(entityHandle, ThreadLocalRandom.current()));
            }
            final String typeName = livingEntity.getType().name();
            if (!typeName.contains("SKELETON") && !typeName.contains("ZOMBIE"))
                return;

            livingEntity.getEquipment().clear();

            Object world = worldField.get(entityHandle);
            Object damageScaler = getDamageScalerMethod.invoke(
                    world,
                    blockPositionConstructor.newInstance(entityHandle));

            regenerateEquipmentMethod.invoke(entityHandle, damageScaler);
            regenerateEquipmentEnchantsMethod.invoke(entityHandle, damageScaler);

        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //Uses reflection for cross version loot table getting.
    public static List<ItemStack> getItemDrops(Object entityHandle, Player killer, List<ItemStack> target) {
        List<ItemStack> items = target == null ? new ArrayList<>() : target;

        try {
            LivingEntity livingEntity = (LivingEntity) getBukkitEntityMethod.invoke(entityHandle);

            items.addAll(CustomDrop.generateCustomDrops(livingEntity));

            int lootingLevel = 0;

            if (killer != null) {
                ItemStack item = killer.getItemInHand();
                if (item != null)
                    lootingLevel = item.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
            }

            ThreadLocalRandom random = ThreadLocalRandom.current();

            dropsField.set(entityHandle, items);

            if (addDropsMethod != null) {
                Object lastDamageSource;

                Object combatTracker = getCombatTrackerMethod.invoke(entityHandle);
                List<?> combatEntryList = (List<?>) combatEntryListField.get(combatTracker);
                if (combatEntryList.isEmpty()) {
                    lastDamageSource = genericDamageSourceField.get(null);
                } else {
                    Object lastEntry = combatEntryList.get(combatEntryList.size() - 1);
                    lastDamageSource = damageSourceField.get(lastEntry);
                }

                addDropsMethod.invoke(entityHandle, true, lootingLevel, lastDamageSource);
            } else {
                dropDeathLootMethod.invoke(entityHandle, true, lootingLevel);
                dropEquipmentMethod.invoke(entityHandle, true, lootingLevel);
            }

            if (getRareDropMethod != null && random.nextFloat() < 0.025F + (float) lootingLevel * 0.01F)
                getRareDropMethod.invoke(entityHandle);

            dropsField.set(entityHandle, new ArrayList<>());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    public static boolean isSimilar(Entity one, Entity two) {
        if (one.getType() != two.getType())
            return false;

        if (Stacker.getStackCount(one) == 1 && one.getCustomName() != null)
            return false;

        if (Stacker.getStackCount(two) == 1 && two.getCustomName() != null)
            return false;

        if (one instanceof Ageable) {
            Ageable ageableOne = (Ageable) one;
            Ageable ageableTwo = (Ageable) two;
            if (ageableOne.isAdult() != ageableTwo.isAdult())
                return false;
        }

        if (one instanceof Villager) {

            Villager villagerOne = (Villager) one;
            Villager villagerTwo = (Villager) two;

            if (villagerOne.getProfession() != villagerTwo.getProfession())
                return false;
        }

        if (one instanceof Tameable) {
            Tameable tameableOne = (Tameable) one;
            Tameable tameableTwo = (Tameable) two;

            if (tameableOne.isTamed() != tameableTwo.isTamed())
                return false;

            if (tameableOne.getOwner().getUniqueId() != tameableTwo.getOwner().getUniqueId())
                return false;
        }

        if (one instanceof Sheep) {
            Sheep sheepOne = (Sheep) one;
            Sheep sheepTwo = (Sheep) two;

            if (sheepOne.isSheared() != sheepTwo.isSheared())
                return false;

            if (sheepOne.getColor() != sheepTwo.getColor())
                return false;
        }

        if (one instanceof Skeleton) {
            Skeleton skeletonOne = (Skeleton) one;
            Skeleton skeletonTwo = (Skeleton) two;

            if (skeletonOne.getSkeletonType() != skeletonTwo.getSkeletonType())
                return false;
        }


        if (one instanceof LivingEntity) {
            LivingEntity livingOne = (LivingEntity) one;
            LivingEntity livingTwo = (LivingEntity) two;

            EntityEquipment oneEquipment = livingOne.getEquipment();
            EntityEquipment twoEquipment = livingTwo.getEquipment();

            if (!oneEquipment.getHelmet().isSimilar(twoEquipment.getHelmet()))
                return false;

            if (!oneEquipment.getChestplate().isSimilar(twoEquipment.getChestplate()))
                return false;

            if (!oneEquipment.getLeggings().isSimilar(twoEquipment.getLeggings()))
                return false;

            if (!oneEquipment.getBoots().isSimilar(twoEquipment.getBoots()))
                return false;

            if (!oneEquipment.getItemInHand().isSimilar(twoEquipment.getItemInHand()))
                return false;
        }

        return true;
    }
}
