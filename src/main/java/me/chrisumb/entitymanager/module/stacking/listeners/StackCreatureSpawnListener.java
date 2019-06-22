package me.chrisumb.entitymanager.module.stacking.listeners;

import me.chrisumb.entitymanager.util.EntityData;
import me.chrisumb.entitymanager.module.stacking.Stacker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class StackCreatureSpawnListener implements Listener {

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		LivingEntity entity = event.getEntity();
		CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();
		EntityData.set(entity, "spawn-reason", spawnReason);
		Stacker.attemptStackEntity(entity);
	}
}
