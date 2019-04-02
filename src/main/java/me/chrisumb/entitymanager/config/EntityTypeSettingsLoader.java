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
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public final class EntityTypeSettingsLoader {

	private EntityTypeSettingsLoader() {
	}

	public static EntityTypeSettings loadFrom(String entityTypeName) {
		FileConfiguration cfg = EntityManagerPlugin.getInstance().getConfig();
		ConfigurationSection section = cfg.getConfigurationSection("entities." + entityTypeName);

		if (section == null)
			return null;

		ConfigurationSection defaultSection = cfg.getConfigurationSection("entities.default");
		EntityTypeSettings settings = new EntityTypeSettings();

		loadStacking(entityTypeName, section, defaultSection, settings);
		loadCustomDrops(entityTypeName, section, defaultSection, settings);

		return settings;
	}

	private static void loadCustomDrops(String entityTypeName, ConfigurationSection section, ConfigurationSection defaultSection, EntityTypeSettings settings) {
		ConfigurationSection dropsSection = section.getConfigurationSection("custom-drops");
		Defaults defaults = new Defaults(dropsSection, null);
		if (!entityTypeName.equalsIgnoreCase("default"))
			defaults.b = defaultSection.getConfigurationSection("custom-drops");

		if(defaults.a == null && defaults.b == null)
			return;

		List<CustomDrop> drops = new ArrayList<>();

//		List<Map<?, ?>> items = dropsSection.getMapList("items");
		List<Map<?, ?>> items = defaults.get("items", ConfigurationSection::getMapList);
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

	private static void loadStacking(String entityTypeName, ConfigurationSection section, ConfigurationSection defaultSection, EntityTypeSettings settings) {
		ConfigurationSection stackingSection = section.getConfigurationSection("stacking");
		Defaults defaults = new Defaults(stackingSection, null);
		if (!entityTypeName.equalsIgnoreCase("default"))
			defaults.b = defaultSection.getConfigurationSection("stacking");

		if(defaults.a == null && defaults.b == null)
			return;

		if (defaults.contains("exempt") && defaults.get("exempt", ConfigurationSection::getBoolean))
			return;

		int limit = defaults.get("limit", ConfigurationSection::getInt);
		int radius = defaults.get("radius", ConfigurationSection::getInt);
		boolean multiplyDrops = defaults.get("multiply-drops", ConfigurationSection::getBoolean);
		boolean stackDiverse = defaults.get("stack-diverse", ConfigurationSection::getBoolean);
		boolean doDeathAnimation = defaults.get("death-animation", ConfigurationSection::getBoolean);
		String stackNameFormat = defaults.get("stack-name", ConfigurationSection::getString);

		Map<EntityDamageEvent.DamageCause, Integer> deathCauseCounts = new HashMap<>();
		List<CreatureSpawnEvent.SpawnReason> invalidSpawnReasons = new ArrayList<>();

		defaults.optional("invalid-spawn-reasons", ConfigurationSection::getStringList)
				.map(it -> it.stream().map(key -> parse(CreatureSpawnEvent.SpawnReason.class, key)))
				.ifPresent(it -> it.collect(Collectors.toCollection(() -> invalidSpawnReasons)));

//		List<String> invalidSpawnReasonsNames = defaults.get("invalid-spawn-reasons", ConfigurationSection::getStringList);
//		if (invalidSpawnReasonsNames != null) {
//			for (String key : invalidSpawnReasonsNames) {
//				invalidSpawnReasons.add(parse(CreatureSpawnEvent.SpawnReason.class, key));
//			}
//		}

		int defaultDeathCount = 1;

		ConfigurationSection deathCounts = defaults.get("stack-death-count", ConfigurationSection::getConfigurationSection);
		if (deathCounts != null) {
			if (deathCounts.contains("default"))
				defaultDeathCount = Math.min(0, deathCounts.getInt("default"));

			Set<String> keys = deathCounts.getKeys(false);
			for (String key : keys) {
				deathCauseCounts.put(parse(EntityDamageEvent.DamageCause.class, key), deathCounts.getInt(key));
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

	private static <T extends Enum<T>> T parse(Class<T> clazz, String name) {
		try {
			return Enum.valueOf(clazz, name);
		}catch (IllegalArgumentException e) {
			return null;
		}
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

		if (name != null)
			meta.setDisplayName(StringUtil.color(name));

		List<String> lore = new ArrayList<>();

		for (String s : displayLore)
			lore.add(StringUtil.color(s));

		meta.setLore(lore);

		if (flags.contains("glowing") && !meta.hasEnchants()) {
			meta.addEnchant(Enchantment.LURE, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}

		item.setItemMeta(meta);

		for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet())
			item.addUnsafeEnchantment(entry.getKey(), entry.getValue());

		return item;
	}

	private static Map<Enchantment, Integer> parseEnchants(ConfigurationSection enchantsSection) {
		final Map<Enchantment, Integer> result = new HashMap<>();

		Set<String> keys = enchantsSection.getKeys(false);

		for (String enchantName : keys) {

			Enchantment enchant = Enchantment.getByName(enchantName);
			if (enchant == null)
				continue;

			int level = enchantsSection.getInt(enchantName);
			result.put(enchant, level);
		}

		return result;
	}

	private static class Defaults {
		public ConfigurationSection b;
		private ConfigurationSection a;

		public Defaults(ConfigurationSection a, ConfigurationSection b) {
			this.a = a;
			this.b = b;
		}

		public boolean contains(String name) {
			return (a != null && a.contains(name)) || (b != null && b.contains(name));
		}

		public <T> T get(String name, BiFunction<ConfigurationSection, String, T> function) {
			if (a.contains(name)) {
				final T valueA = function.apply(a, name);
				if (valueA != null)
					return valueA;
			}

			if (b != null && b.contains(name))
				return function.apply(b, name);

			return null;
		}

		public <T> Optional<T> optional(String name, BiFunction<ConfigurationSection, String, T> function) {
			return Optional.ofNullable(get(name, function));
		}
	}


}

