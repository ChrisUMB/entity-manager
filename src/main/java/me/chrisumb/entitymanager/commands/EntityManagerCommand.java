package me.chrisumb.entitymanager.commands;

import blue.sparse.bshade.command.CommandContext;
import blue.sparse.bshade.command.CommandGroup;
import blue.sparse.bshade.command.Commands;
import blue.sparse.bshade.util.StringUtil;
import me.chrisumb.entitymanager.EntityManagerPlugin;
import me.chrisumb.entitymanager.config.EntityTypeSettings;
import me.chrisumb.entitymanager.module.stacking.Stacker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Commands.Name("entitymanager")
@Commands.Aliases("em")
public class EntityManagerCommand implements CommandGroup {

	public void spawn(
			CommandContext<Player> context,
			@Stackable EntityType type,
			int count
	) {
		if (!context.sender.hasPermission("entitymanager.admin"))
			return;

		Entity entity = context.sender.getWorld().spawnEntity(context.sender.getLocation(), type);
		Stacker.setStackCount((LivingEntity) entity, count);
	}

	public void reload(
			CommandContext<Player> context
	) {
		if (!context.sender.hasPermission("entitymanager.admin"))
			return;

		try {
			EntityManagerPlugin.getInstance().reloadConfig();
			EntityTypeSettings.loadAll();
			context.sender.sendMessage(StringUtil.color("&aSuccessfully reloaded configuration for EntityManager!"));
		} catch (Throwable t) {
			t.printStackTrace();
			context.sender.sendMessage(StringUtil.color("&cFailed to reload configuration for EntityManager!"));
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Stackable {
	}

}
