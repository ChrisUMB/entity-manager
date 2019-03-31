package me.chrisumb.entitymanager.util;

import blue.sparse.bshade.util.StringUtil;
import me.chrisumb.entitymanager.EntityManagerPlugin;
import me.chrisumb.entitymanager.EntityTypeSettings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Stacker {
	private static boolean hasBegunStacker = false;

	private Stacker() {
	}

	public static void startStackTask() {
		if (hasBegunStacker)
			return;

		EntityManagerPlugin plugin = EntityManagerPlugin.getInstance();
		final FileConfiguration config = plugin.getConfig();
		int frequency = config.getInt("options.stacking.frequency");
		int count = config.getInt("options.stacking.count");

		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
			List<World> worlds = Bukkit.getWorlds();
			ThreadLocalRandom random = ThreadLocalRandom.current();

			//Attempts is for failed attempts to find valid entities to prevent server freezing.
			int attempts = 0;
			for (int i = 0; i < count; i++) {
				World world = worlds.get(random.nextInt(worlds.size()));
				List<LivingEntity> livingEntities = world.getLivingEntities();
				if (livingEntities.isEmpty())
					continue;

				LivingEntity entity = livingEntities.get(random.nextInt(livingEntities.size()));

				if (!entity.isValid()) {
					i--;
					if (++attempts >= 15)
						break;
				} else {
					attemptStackEntity(entity);
					attempts = 0;
				}

			}

		}, frequency, frequency);

		hasBegunStacker = true;
	}

	public static void attemptStackEntity(LivingEntity entity) {
		EntityTypeSettings settings = EntityTypeSettings.getSettings(entity.getType());
		if (settings == null)
			return;

		EntityTypeSettings.Stacking stackingSettings = settings.getStacking();

		if (stackingSettings == null) {
			return;
		}

		int radius = stackingSettings.getRadius();

		List<LivingEntity> nearbyEntitiesList = entity
				.getNearbyEntities(radius, radius, radius)
				.stream().filter(it -> it instanceof LivingEntity && !it.isDead() && it.isValid())
				.map(it -> (LivingEntity) it)
				.collect(Collectors.toList());

		nearbyEntitiesList.add(entity);

		Stream<LivingEntity> nearbyEntities = nearbyEntitiesList
				.stream().filter(it -> it.getType() == entity.getType());

		if (!stackingSettings.stackDiverse())
			nearbyEntities = nearbyEntities.filter(it -> EntityUtil.isSimilar(entity, it));

		nearbyEntitiesList = nearbyEntities
				.filter(it -> getStackCount(it) < stackingSettings.getLimit())
				.sorted(Comparator.comparingInt(Stacker::getStackCount))
				.collect(Collectors.toList());

		if (nearbyEntitiesList.isEmpty())
			return;

		LivingEntity primaryEntity = nearbyEntitiesList.remove(nearbyEntitiesList.size() - 1);

		int stackCount = getStackCount(primaryEntity);

		for (LivingEntity other : nearbyEntitiesList) {
			stackCount += getStackCount(other);
			setStackCount(primaryEntity, stackCount);

			if (stackCount > stackingSettings.getLimit()) {
				setStackCount(primaryEntity, stackingSettings.getLimit());
				setStackCount(other, stackCount - stackingSettings.getLimit());
				updateStackName(primaryEntity);
				primaryEntity = other;
				stackCount = getStackCount(primaryEntity);
			} else {
				other.remove();
			}
		}

		updateStackName(primaryEntity);
	}

	public static void updateStackName(LivingEntity entity) {
		EntityTypeSettings settings = EntityTypeSettings.getSettings(entity.getType());

		if (settings == null)
			return;

		EntityTypeSettings.Stacking stackingSettings = settings.getStacking();

		if (stackingSettings == null)
			return;

		String format = stackingSettings.getStackNameFormat();
		int stackCount = getStackCount(entity);

		String formatted = StringUtil.placeholders(
				format, true,
				"type", StringUtil.titleCase(entity.getType().name()),
				"count", StringUtil.commas(stackCount)
		);

		if (stackCount > 1) {
			entity.setCustomName(formatted);
			entity.setCustomNameVisible(true);
		} else {
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}

	public static int getStackCount(LivingEntity entity) {
		if (!entity.hasMetadata("stackCount"))
			return 1;

		return EntityData.getInt(entity, "stackCount");
	}

	public static void setStackCount(LivingEntity entity, int amount) {
		if(amount <= 1)
			EntityData.remove(entity, "stackCount");
		else
			EntityData.set(entity, "stackCount", amount);
	}

}
