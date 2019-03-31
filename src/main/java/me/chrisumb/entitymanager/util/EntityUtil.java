package me.chrisumb.entitymanager.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class EntityUtil {

	private static Method getCombatTrackerMethod = null;
	private static Method dropDeathLootMethod = null;
	private static Method getExpRewardMethod = null;
	private static Method getRareDropMethod = null;
	private static Method getHandleMethod = null;
	private static Method addDropsMethod = null;

	private static Field combatEntryListField = null;
	private static Field damageSourceField = null;
	private static Field dropsField = null;

	static {
		try {
			Class<?> elClass = ReflectionUtil.getVanillaClass("EntityLiving");
			getHandleMethod = ReflectionUtil.getCraftClass("entity.CraftLivingEntity").getDeclaredMethod("getHandle");

			dropsField = elClass.getDeclaredField("drops");
			dropsField.setAccessible(true);

			dropDeathLootMethod = elClass.getDeclaredMethod("dropDeathLoot", boolean.class, int.class);
			dropDeathLootMethod.setAccessible(true);

			getExpRewardMethod = elClass.getDeclaredMethod("getExpReward");
			getExpRewardMethod.setAccessible(true);

			try {
				getCombatTrackerMethod = elClass.getDeclaredMethod("getCombatTracker");
				getCombatTrackerMethod.setAccessible(true);
			} catch (NoSuchMethodException ignored) {

			}

			try {
				combatEntryListField = ReflectionUtil.getVanillaClass("CombatTracker").getDeclaredField("a");
				combatEntryListField.setAccessible(true);
			} catch (NoSuchFieldException ignored) {

			}

			try {
				damageSourceField = ReflectionUtil.getVanillaClass("CombatEntry").getDeclaredField("a");
				damageSourceField.setAccessible(true);
			} catch (NoSuchFieldException ignored) {

			}

			try {
				addDropsMethod = elClass.getDeclaredMethod("a",
						boolean.class,
						int.class,
						ReflectionUtil.getVanillaClass("DamageSource"));
				addDropsMethod.setAccessible(true);
			} catch (NoSuchMethodException ignored) {
			}

			try {
				getRareDropMethod = elClass.getDeclaredMethod("getRareDrop");
				getRareDropMethod.setAccessible(true);
			} catch (NoSuchMethodException ignored) {
			}


		} catch (Exception e) {
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


	public static int getExperienceDrops(LivingEntity entity) {
		return getExperienceDrops(getEntityHandle(entity));
	}

		//Reflection for getting experience drops.
	public static int getExperienceDrops(Object entityHandle) {
		try {
			return (int) getExpRewardMethod.invoke(entityHandle);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static List<ItemStack> getItemDrops(LivingEntity entity) {
		return getItemDrops(getEntityHandle(entity), entity.getKiller(), null);
	}

	//Uses reflection for cross version loot table getting.
	public static List<ItemStack> getItemDrops(Object entityHandle, Player killer, List<ItemStack> target) {
		List<ItemStack> items = target == null ? new ArrayList<>() : target;

		try {
			int lootingLevel = 0;

			if (killer != null) {
				ItemStack item = killer.getItemInHand();
				if (item != null)
					lootingLevel = item.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
			}

			ThreadLocalRandom random = ThreadLocalRandom.current();

			dropsField.set(entityHandle, items);

			if (addDropsMethod != null) {
				Object combatTracker = getCombatTrackerMethod.invoke(entityHandle);
				List<?> combatEntryList = (List<?>) combatEntryListField.get(combatTracker);
				Object lastEntry = combatEntryList.get(combatEntryList.size() - 1);
				addDropsMethod.invoke(entityHandle, true, lootingLevel, damageSourceField.get(lastEntry));
			} else {
				dropDeathLootMethod.invoke(entityHandle, true, lootingLevel);
			}

			if (getRareDropMethod != null && random.nextFloat() < 0.025F + (float) lootingLevel * 0.01F)
				getRareDropMethod.invoke(entityHandle);

			dropsField.set(entityHandle, null);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return items;
	}

	public static boolean isSimilar(Entity one, Entity two) {
		if (one.getType() != two.getType())
			return false;

		if (!Objects.equals(one.getCustomName(), two.getCustomName()))
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
