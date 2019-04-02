package me.chrisumb.entitymanager;

import blue.sparse.bshade.command.Commands;
import blue.sparse.bshade.command.parameters.filter.ParameterFilters;
import me.chrisumb.entitymanager.commands.EntityManagerCommand;
import me.chrisumb.entitymanager.config.EntityTypeSettings;
import me.chrisumb.entitymanager.module.drops.listeners.DropEntityDeathListener;
import me.chrisumb.entitymanager.module.stacking.listeners.StackCreatureSpawnListener;
import me.chrisumb.entitymanager.module.stacking.listeners.StackEntityDeathListener;
import me.chrisumb.entitymanager.module.stacking.Stacker;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EntityManagerPlugin extends JavaPlugin {

    private static EntityManagerPlugin instance;

    public EntityManagerPlugin() {
        if(instance != null)
            throw new IllegalPluginAccessException("Plugin already loaded.");

        instance = this;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        registerListeners();
        registerCommands();
        EntityTypeSettings.loadAll();
        Stacker.startStackTask();
    }

    private void registerListeners() {
        final PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new StackCreatureSpawnListener(), this);
        pluginManager.registerEvents(new StackEntityDeathListener(), this);
        pluginManager.registerEvents(new DropEntityDeathListener(), this);
    }

    private void registerCommands() {
        ParameterFilters.register(
                EntityManagerCommand.Stackable.class,
                EntityType.class,
                (stackable, entityType) -> {
                    EntityTypeSettings settings = EntityTypeSettings.getSettings(entityType);
                    if(settings == null)
                        return false;

                    return settings.getStacking() != null;
                }
        );

        Commands.registerCommands(new EntityManagerCommand());
    }

    public static EntityManagerPlugin getInstance() {
        return instance;
    }
}
