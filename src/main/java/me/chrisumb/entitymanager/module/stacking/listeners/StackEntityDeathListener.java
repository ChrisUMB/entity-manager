package me.chrisumb.entitymanager.module.stacking.listeners;

import blue.sparse.bshade.util.ItemStackUtil;
import blue.sparse.bshade.util.StackingItemList;
import me.chrisumb.entitymanager.config.EntityTypeSettings;
import me.chrisumb.entitymanager.module.drops.CustomDrop;
import me.chrisumb.entitymanager.util.EntityUtil;
import me.chrisumb.entitymanager.module.stacking.Stacker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StackEntityDeathListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        Player killer = null;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent event1 = (EntityDamageByEntityEvent) event;
            Entity damager = event1.getDamager();
            if (damager instanceof Player) {
                killer = (Player) damager;
            }
        }

        LivingEntity entity = (LivingEntity) event.getEntity();
        EntityTypeSettings.Stacking stackingSettings = getStackingSettings(entity);

        if (stackingSettings == null)
            return;

        if (entity.getHealth() > event.getFinalDamage())
            return;

        int count = Stacker.getStackCount(entity);

        if (count == 1)
            return;

        entity.setLastDamageCause(event);

        final boolean doDeathAnimation = stackingSettings.doDeathAnimation();
        if (!kill(entity, killer, doDeathAnimation) && !doDeathAnimation) {
            event.setDamage(0.0);
            entity.setHealth(entity.getMaxHealth());
        }
    }

    private boolean kill(LivingEntity entity, Player killer, boolean didEntityDie) {
        EntityTypeSettings.Stacking stackingSettings = getStackingSettings(entity);

        if (stackingSettings == null)
            return false;

        int count = Stacker.getStackCount(entity);

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

        count -= toKill;

        if (stackingSettings.isMultiplyDrops()) {
            if (count > 1 && !stackingSettings.doDeathAnimation()) {
                multiplyDrops(toKill, entity, killer);
            } else {
                multiplyDrops(toKill - 1, entity, killer);
            }
        }

        if (count >= 1) {
            LivingEntity newEntity = didEntityDie ? (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType()) : entity;
            EntityUtil.regenerateEquipment(EntityUtil.getEntityHandle(newEntity));
            Stacker.setStackCount(newEntity, count);
        } else if (didEntityDie) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }

        return count <= 0;
    }

    private EntityTypeSettings.Stacking getStackingSettings(LivingEntity entity) {
        EntityTypeSettings settings = EntityTypeSettings.getSettings(entity.getType());
        if (settings == null)
            return null;

        return settings.getStacking();
    }

    private void multiplyDrops(int count, LivingEntity entity, Player killer) {
        if (count <= 0)
            return;

        Location loc = entity.getLocation();
        List<ItemStack> drops = new StackingItemList();

        int experience = 0;

        Object handle = EntityUtil.getEntityHandle(entity);

        for (int i = 0; i < count; i++) {
            final List<ItemStack> newDrops = EntityUtil.getItemDrops(handle, killer, new ArrayList<>());
            newDrops.stream()
                    .filter(it -> it != null && it.getType() != Material.AIR && it.getAmount() > 0)
                    .collect(Collectors.toCollection(() -> drops));
            experience += EntityUtil.getExperienceDrops(entity, killer);
            EntityUtil.regenerateEquipment(handle);
        }

        ItemStackUtil.dropAll(drops, entity.getLocation(), false, true);

        if (experience > 0)
            loc.getWorld().spawn(loc, ExperienceOrb.class).setExperience(experience);
    }
}
