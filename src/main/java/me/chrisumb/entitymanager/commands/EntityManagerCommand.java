package me.chrisumb.entitymanager.commands;

import blue.sparse.bshade.command.CommandContext;
import blue.sparse.bshade.command.CommandGroup;
import blue.sparse.bshade.command.Commands;
import blue.sparse.bshade.util.StringUtil;
import me.chrisumb.entitymanager.EntityManagerPlugin;
import me.chrisumb.entitymanager.util.Stacker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Commands.Name("entitymanager")
@Commands.Aliases("em")
public class EntityManagerCommand implements CommandGroup {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Stackable { }

    public void spawn(
            CommandContext<Player> context,
            @Stackable EntityType type,
            int count
    ) {
        Entity entity = context.sender.getWorld().spawnEntity(context.sender.getLocation(), type);
        Stacker.setStackCount((LivingEntity) entity, count);
    }

    public void reload(
            CommandContext<Player> context
    ) {
        if (!context.sender.hasPermission("entitymanager.reload"))
            return;

        EntityManagerPlugin.getInstance().reloadConfig();
        context.sender.sendMessage(StringUtil.color("&aSuccessfully reloaded configuration for EntityManager!"));
    }

}
