package me.chrisumb.entitymanager;

import blue.sparse.bshade.command.Commands;
import blue.sparse.bshade.command.parameters.filter.ParameterFilters;
import me.chrisumb.entitymanager.commands.EntityManagerCommand;
import me.chrisumb.entitymanager.listeners.CreatureSpawnListener;
import me.chrisumb.entitymanager.listeners.EntityDeathListener;
import me.chrisumb.entitymanager.util.Stacker;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EntityManagerPlugin extends JavaPlugin {

    private static EntityManagerPlugin instance;

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
        pluginManager.registerEvents(new CreatureSpawnListener(), this);
        pluginManager.registerEvents(new EntityDeathListener(), this);
    }

    private void registerCommands() {
        ParameterFilters.register(
                EntityManagerCommand.Stackable.class,
                EntityType.class,
                (stackable, entityType) -> {
                    System.out.println("Checking entity type "+entityType);
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
