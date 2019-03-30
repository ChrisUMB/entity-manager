package me.chrisumb.entitymanager.util;

import me.chrisumb.entitymanager.EntityManagerPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public final class EntityData {

    private EntityData() {
    }

    public static void set(Entity entity, String key, Object value) {
        entity.setMetadata(key, new FixedMetadataValue(EntityManagerPlugin.getInstance(), value));
    }

    public static Object get(Entity entity, String key) {
        return entity.getMetadata(key).stream()
                .filter(it -> it.getOwningPlugin() == EntityManagerPlugin.getInstance())
                .findFirst()
                .map(MetadataValue::value)
                .orElse(null);
    }

    public static int getInt(Entity entity, String key) {
        return (int) get(entity, key);
    }
}