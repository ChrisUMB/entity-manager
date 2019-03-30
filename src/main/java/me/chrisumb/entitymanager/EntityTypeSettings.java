package me.chrisumb.entitymanager;

import me.chrisumb.entitymanager.config.EntityTypeSettingsLoader;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityTypeSettings {

    private static Map<EntityType, EntityTypeSettings> settingsCache = new HashMap<>();
    private static EntityTypeSettings defaultEntityTypeSettings = EntityTypeSettingsLoader.loadFrom("default");

    private Stacking stacking;
    private CustomDrops customDrops;
    private CustomSpawns customSpawns;

    //If this is null, the entity is exempt from stacking.
    public Stacking getStacking() {
        return stacking;
    }

    public CustomDrops getCustomDrops() {
        return customDrops;
    }

    public CustomSpawns getCustomSpawns() {
        return customSpawns;
    }

    public void setStacking(Stacking stacking) {
        this.stacking = stacking;
    }

    public void setCustomDrops(CustomDrops customDrops) {
        this.customDrops = customDrops;
    }

    public void setCustomSpawns(CustomSpawns customSpawns) {
        this.customSpawns = customSpawns;
    }

    public static void registerSettings(EntityType type, EntityTypeSettings settings) {
        settingsCache.put(type, settings);
    }

    public static EntityTypeSettings getSettings(EntityType type) {
        if (!type.isSpawnable() || type == EntityType.PLAYER || !type.isAlive())
            return null;

        if (!settingsCache.containsKey(type))
            return getDefault();

        return settingsCache.get(type);
    }

    public static void loadAll() {
        for (EntityType type : EntityType.values()) {
            EntityTypeSettings settings = EntityTypeSettingsLoader.loadFrom(type.name());
            if (settings == null || !type.isSpawnable() || type == EntityType.PLAYER || !type.isAlive())
                continue;

            EntityTypeSettings.registerSettings(type, settings);
        }
    }

    public static EntityTypeSettings getDefault() {
        return defaultEntityTypeSettings;
    }

    public class Stacking {
        private int limit;
        private int radius;
        private boolean multiplyDrops;
        private boolean stackDiverse;
        private List<EntityDamageEvent.DamageCause> stackDeathCauses;
        private String stackNameFormat;

        public Stacking(int limit,
                        int radius,
                        boolean multiplyDrops,
                        boolean mergeDiverse,
                        List<EntityDamageEvent.DamageCause> stackDeathCauses,
                        String stackNameFormat
        ) {
            this.limit = limit;
            this.radius = radius;
            this.multiplyDrops = multiplyDrops;
            this.stackDiverse = mergeDiverse;
            this.stackDeathCauses = stackDeathCauses;
            this.stackNameFormat = stackNameFormat;
        }

        public int getLimit() {
            return limit;
        }

        public int getRadius() {
            return radius;
        }

        public boolean isMultiplyDrops() {
            return multiplyDrops;
        }

        public boolean stackDiverse() {
            return stackDiverse;
        }

        public List<EntityDamageEvent.DamageCause> getStackDeathCauses() {
            return stackDeathCauses;
        }

        public String getStackNameFormat() {
            return stackNameFormat;
        }
    }

    public class CustomDrops {

    }

    public class CustomSpawns {

    }

}
