package me.chrisumb.entitymanager.config;

import me.chrisumb.entitymanager.EntityManagerPlugin;
import me.chrisumb.entitymanager.EntityTypeSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

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
            String stackNameFormat = stackingSection.getString("stack-name");

            Map<EntityDamageEvent.DamageCause, Integer> deathCauseCounts = new HashMap<>();
            int defaultDeathCount = 1;

            if(stackingSection.contains("death-count")) {
                ConfigurationSection deathCounts = stackingSection.getConfigurationSection("death-count");
                if(deathCounts.contains("default"))
                    defaultDeathCount = Math.min(0, deathCounts.getInt("default"));

                Set<String> keys = deathCounts.getKeys(false);
                for(String key : keys) {
                    try {
                        deathCauseCounts.put(EntityDamageEvent.DamageCause.valueOf(key), deathCounts.getInt(key));
                    }catch (IllegalArgumentException ignored) {}
                }
            }

            settings.setStacking(
                    settings.new Stacking(
                            limit,
                            radius,
                            multiplyDrops,
                            stackDiverse,
                            doDeathAnimation,
                            defaultDeathCount,
                            deathCauseCounts,
                            stackNameFormat
                    )
            );
        }

        return settings;
    }


}

