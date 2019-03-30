package me.chrisumb.entitymanager.listeners;

import blue.sparse.bshade.util.ItemStackUtil;
import blue.sparse.bshade.util.MergingItemStackList;
import me.chrisumb.entitymanager.EntityTypeSettings;
import me.chrisumb.entitymanager.util.EntityUtil;
import me.chrisumb.entitymanager.util.Stacker;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EntityDeathListener implements Listener {

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        EntityTypeSettings settings = EntityTypeSettings.getSettings(entity.getType());
        if (settings == null)
            return;

        EntityTypeSettings.Stacking stackingSettings = settings.getStacking();

        if (stackingSettings == null) {
            return;
        }

        int count = Stacker.getStackCount(entity);

        if (count >= 1 && entity.getCustomName() != null) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }

        EntityDamageEvent.DamageCause damageCause = entity.getLastDamageCause().getCause();

        if (stackingSettings.getStackDeathCauses().contains(damageCause)) {
            if (stackingSettings.isMultiplyDrops()) {
                multiplyDrops(count - 1, entity);
                return;
            }

            return;
        }

        if (count > 1) {
            LivingEntity newEntity = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());
            Stacker.setStackCount(newEntity, count - 1);
            Stacker.updateStackName(newEntity);
        }
    }

    private void multiplyDrops(int count, LivingEntity entity) {
        if (count <= 0)
            return;


        Location loc = entity.getLocation();
        List<ItemStack> drops = new MergingItemStackList();

        int experience = 0;

        for (int i = 0; i < count; i++) {
            drops.addAll(EntityUtil.getItemDrops(entity));
            experience += EntityUtil.getExperienceDrops(entity);
        }

        ItemStackUtil.dropAll(drops, entity.getLocation(), false, true);

        if(experience> 0) {
            ExperienceOrb spawn = loc.getWorld().spawn(loc, ExperienceOrb.class);
            spawn.setExperience(experience);
        }
    }
}
