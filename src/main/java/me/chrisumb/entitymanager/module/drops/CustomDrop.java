package me.chrisumb.entitymanager.module.drops;

import me.chrisumb.entitymanager.config.EntityTypeSettings;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CustomDrop {

	private ItemStack item;
	private double dropChance;
	private int amountMin;
	private int amountMax;

	public CustomDrop(ItemStack item, double dropChance, int amountMin, int amountMax) {
		this.item = item;
		this.dropChance = dropChance;
		this.amountMin = Math.max(amountMin, 1);
		this.amountMax = Math.max(amountMin, amountMax);
	}

	public static List<ItemStack> generateCustomDrops(LivingEntity livingEntity) {
		EntityTypeSettings settings = EntityTypeSettings.getSettings(livingEntity.getType());
		if(settings == null)
			return Collections.emptyList();

		EntityTypeSettings.CustomDrops dropsSettings = settings.getCustomDrops();
		if(dropsSettings == null)
			return Collections.emptyList();

		List<ItemStack> result = new ArrayList<>();
		List<CustomDrop> customDrops = dropsSettings.getItems();

		for (CustomDrop drop : customDrops) {
			ItemStack item = drop.getDrop();
			if (item != null)
				result.add(item);
		}

		return result;
	}

	public ItemStack getDrop() {
		ThreadLocalRandom random = ThreadLocalRandom.current();

		if (random.nextDouble() > dropChance)
			return null;

		ItemStack clone = item.clone();
		clone.setAmount(random.nextInt(amountMin, amountMax + 1));
		return clone;
	}
}
