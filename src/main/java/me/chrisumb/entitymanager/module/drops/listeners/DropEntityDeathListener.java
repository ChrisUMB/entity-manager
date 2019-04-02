package me.chrisumb.entitymanager.module.drops.listeners;

import blue.sparse.bshade.util.ItemStackUtil;
import me.chrisumb.entitymanager.module.drops.CustomDrop;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DropEntityDeathListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStackUtil.dropAll(CustomDrop.generateCustomDrops(entity), entity.getLocation(), true, true);
    }
}
