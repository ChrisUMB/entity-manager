package me.chrisumb.entitymanager.module.drops.listeners;

import me.chrisumb.entitymanager.module.drops.CustomDrop;
import me.chrisumb.entitymanager.module.stacking.Stacker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropEntityDeathListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();

        int stack = Stacker.getStackCount(entity);
        if (stack <= 1) {
            List<ItemStack> itemStacks = CustomDrop.generateCustomDrops(entity);
            for (ItemStack itemStack : itemStacks)
                entity.getWorld().dropItemNaturally(entity.getLocation(), itemStack);
        }
    }
}
