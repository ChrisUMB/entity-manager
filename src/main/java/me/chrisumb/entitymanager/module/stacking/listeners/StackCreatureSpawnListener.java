package me.chrisumb.entitymanager.module.stacking.listeners;

import me.chrisumb.entitymanager.util.EntityData;
import me.chrisumb.entitymanager.module.stacking.Stacker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class StackCreatureSpawnListener implements Listener {

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		EntityData.set(event.getEntity(), "spawn-reason", event.getSpawnReason());

		if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM)
			return;

		Stacker.attemptStackEntity(event.getEntity());
	}

}
