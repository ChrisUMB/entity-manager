package me.chrisumb.entitymanager.config;

import blue.sparse.bshade.util.StringUtil;
import me.chrisumb.entitymanager.EntityManagerPlugin;
import me.chrisumb.entitymanager.module.drops.CustomDrop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            List<CreatureSpawnEvent.SpawnReason> invalidSpawnReasons = new ArrayList<>();
            int defaultDeathCount = 1;

            if(stackingSection.contains("invalid-spawn-reasons")) {
                List<String> invalidSpawnReasonsNames = stackingSection.getStringList("invalid-spawn-reasons");

                for(String key : invalidSpawnReasonsNames) {
                    try {
                        invalidSpawnReasons.add(CreatureSpawnEvent.SpawnReason.valueOf(key));
                    }catch(IllegalArgumentException ignored){}
                }
            }

            if(stackingSection.contains("stack-death-count")) {
                ConfigurationSection deathCounts = stackingSection.getConfigurationSection("stack-death-count");
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
                            stackNameFormat,
                            invalidSpawnReasons
                    )
            );
        }

        if(section.contains("custom-drops")) {
            List<CustomDrop> drops = new ArrayList<>();
            ConfigurationSection dropsSection = section.getConfigurationSection("custom-drops");

            List<Map<?, ?>> items = dropsSection.getMapList("items");
            for (Map<?, ?> map : items) {
                ConfigurationSection itemSection = new YamlConfiguration().createSection("_", map);
                ItemStack item = parseItem(itemSection);
                double chance = itemSection.getDouble("chance");
                int amountMin = itemSection.getInt("amount.min");
                int amountMax = itemSection.getInt("amount.max");
                drops.add(new CustomDrop(item, chance, amountMin, amountMax));
            }

            settings.setCustomDrops(settings.new CustomDrops(drops));
        }

        return settings;
    }

    private static ItemStack parseItem(ConfigurationSection section) {
        Material type = Material.matchMaterial(section.getString("type"));
        int amount = section.isSet("amount") ? section.getInt("amount") : 1;
        byte data = section.isSet("data") ? (byte) section.getInt("data") : 0;
        String name = section.isSet("name") ? section.getString("name") : null;
        List<String> displayLore = section.isSet("lore") ? section.getStringList("lore") : Collections.emptyList();
        List<String> flags = section.isSet("flags") ? section.getStringList("flags") : Collections.emptyList();
        Map<Enchantment, Integer> enchants = section.isSet("enchantments")
                ? parseEnchants(section.getConfigurationSection("enchantments")) : new HashMap<>();

        ItemStack item = new ItemStack(type, amount, data);
        ItemMeta meta = item.getItemMeta();

        if(name != null)
            meta.setDisplayName(StringUtil.color(name));

        List<String> lore = new ArrayList<>();

        for(String s : displayLore)
            lore.add(StringUtil.color(s));

        meta.setLore(lore);

        if(flags.contains("glowing") && !meta.hasEnchants()) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);

        for(Map.Entry<Enchantment, Integer> entry : enchants.entrySet())
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());

        return item;
    }

    private static Map<Enchantment, Integer> parseEnchants(ConfigurationSection enchantsSection) {
        final Map<Enchantment, Integer> result = new HashMap<>();

        Set<String> keys = enchantsSection.getKeys(false);

        for(String enchantName : keys) {

            Enchantment enchant = Enchantment.getByName(enchantName);
            if(enchant == null)
                continue;

            int level = enchantsSection.getInt(enchantName);
            result.put(enchant, level);
        }

        return result;
    }


}

