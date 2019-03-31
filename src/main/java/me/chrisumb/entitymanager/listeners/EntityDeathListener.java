package me.chrisumb.entitymanager.listeners;

import blue.sparse.bshade.util.ItemStackUtil;
import blue.sparse.bshade.util.MergingItemStackList;
import me.chrisumb.entitymanager.EntityTypeSettings;
import me.chrisumb.entitymanager.util.EntityUtil;
import me.chrisumb.entitymanager.util.Stacker;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EntityDeathListener implements Listener {

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity))
			return;

		LivingEntity entity = (LivingEntity) event.getEntity();
		EntityTypeSettings.Stacking stackingSettings = getStackingSettings(entity);

		if (stackingSettings == null)
			return;

		if (stackingSettings.doDeathAnimation())
			return;

		if (entity.getHealth() > event.getFinalDamage())
			return;

		//Cancelling would allow for hitting it SUPER DUPER fast and killing them pretty fuckin' fast.
		// Potential config option: stack-fast-attack

		int count = Stacker.getStackCount(entity);

		if (count == 1)
			return;

		entity.setLastDamageCause(event);
		if (!kill(entity, false)) {
			event.setDamage(0.0);
			entity.setHealth(entity.getMaxHealth());
		}
	}

	@EventHandler
	public void onDeath(EntityDeathEvent event) {
		EntityTypeSettings.Stacking stackingSettings = getStackingSettings(event.getEntity());

		if (stackingSettings == null)
			return;

		if (!stackingSettings.doDeathAnimation())
			return;

		kill(event.getEntity(), true);
	}

	private boolean kill(LivingEntity entity, boolean didEntityDie) {
		EntityTypeSettings.Stacking stackingSettings = getStackingSettings(entity);

		if (stackingSettings == null)
			return false;

		int count = Stacker.getStackCount(entity);

		if (didEntityDie && count >= 1 && entity.getCustomName() != null) {
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}

		EntityDamageEvent.DamageCause damageCause = entity.getLastDamageCause().getCause();
		int maxToKill = stackingSettings.getDamageCauseDeathCount(damageCause);
		int toKill;

		if (maxToKill > 0) {
			toKill = count % maxToKill;
			if (toKill == 0)
				toKill = Math.min(count, maxToKill);
		} else {
			toKill = count;
		}

		if (stackingSettings.isMultiplyDrops()) {
			multiplyDrops(toKill - 1, entity);
		}

		count -= toKill;

		if (count > 1) {
			LivingEntity newEntity = didEntityDie ? (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType()) : entity;

			if (!stackingSettings.doDeathAnimation()) {
				EntityUtil.regenerateEquipment(EntityUtil.getEntityHandle(newEntity));
				multiplyDrops(1, newEntity);
			}

			Stacker.setStackCount(newEntity, count);
			Stacker.updateStackName(newEntity);

			return false;
		} else {
			return count <= 0;
		}
	}

	private EntityTypeSettings.Stacking getStackingSettings(LivingEntity entity) {
		EntityTypeSettings settings = EntityTypeSettings.getSettings(entity.getType());
		if (settings == null)
			return null;

		return settings.getStacking();
	}

	private void multiplyDrops(int count, LivingEntity entity) {
		if (count <= 0)
			return;

		Location loc = entity.getLocation();
		List<ItemStack> drops = new MergingItemStackList();

		int experience = 0;

		Object handle = EntityUtil.getEntityHandle(entity);

		final Player killer = entity.getKiller();
		for (int i = 0; i < count; i++) {
			List<ItemStack> itemDrops = EntityUtil.getItemDrops(handle, killer, new ArrayList<>());
			drops.addAll(itemDrops);
			experience += EntityUtil.getExperienceDrops(entity);
			EntityUtil.regenerateEquipment(handle);
		}

		ItemStackUtil.dropAll(drops, entity.getLocation(), false, true);

		if (experience > 0) {
			loc.getWorld().spawn(loc, ExperienceOrb.class).setExperience(experience);
		}
	}
}
