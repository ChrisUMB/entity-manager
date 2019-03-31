package me.chrisumb.entitymanager.config;

import me.chrisumb.entitymanager.EntityManagerPlugin;
import me.chrisumb.entitymanager.EntityTypeSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public final class EntityTypeSettingsLoader {

    private EntityTypeSettingsLoader() {
    }

    public static EntityTypeSettings loadFrom(String entityTypeName) {
        FileConfiguration cfg = EntityManagerPlugin.getInstance().getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("entities." + entityTypeName);

        if (section == null)
            return null;

        if (!entityTypeName.equalsIgnoreCase("default")) {
            ConfigurationSection defaultSection = cfg.getConfigurationSection("entities.default");

            for (String key : defaultSection.getKeys(false)) {
                section.addDefault(key, defaultSection.get(key));
            }
        }

        ConfigurationSection stackingSection = section.getConfigurationSection("stacking");

        boolean stackingExempt = false;
        if (stackingSection.contains("exempt") && stackingSection.getBoolean("exempt")) {
            stackingExempt = stackingSection.getBoolean("exempt");
        }
        EntityTypeSettings settings = new EntityTypeSettings();

        if (!stackingExempt) {
            int limit = stackingSection.getInt("limit");
            int radius = stackingSection.getInt("radius");
            boolean multiplyDrops = stackingSection.getBoolean("multiply-drops");
            boolean stackDiverse = stackingSection.getBoolean("stack-diverse");
            boolean doDeathAnimation = stackingSection.getBoolean("death-animation");
            List<EntityDamageEvent.DamageCause> stackDeathCauses = new ArrayList<>();
            String stackNameFormat = stackingSection.getString("stack-name");
            int stackDeathLimit = stackingSection.getInt("stack-death-limit");

            if (stackingSection.contains("stack-death")) {
                List<String> damageCauseNames = stackingSection.getStringList("stack-death");
                for (String name : damageCauseNames) {
                    stackDeathCauses.add(EntityDamageEvent.DamageCause.valueOf(name));
                }
            }

            settings.setStacking(
                    settings.new Stacking(
                            limit,
                            radius,
                            multiplyDrops,
                            stackDiverse,
                            doDeathAnimation,
                            stackDeathCauses,
                            stackNameFormat,
                            stackDeathLimit
                    )
            );
        }

        return settings;
    }


}

