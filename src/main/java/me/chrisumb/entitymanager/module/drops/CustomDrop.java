package me.chrisumb.entitymanager.module.drops;

import blue.sparse.bshade.util.ItemStackUtil;
import me.chrisumb.entitymanager.config.EntityTypeSettings;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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
        this.amountMin = amountMin;
        this.amountMax = amountMax;
    }

    public ItemStack getDrop() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (random.nextDouble() > dropChance)
            return null;

        item.setAmount(random.nextInt(amountMin, amountMax + 1));
        return item;
    }

    public static List<ItemStack> generateCustomDrops(LivingEntity livingEntity) {
        List<ItemStack> items = new ArrayList<>();

        EntityTypeSettings settings = EntityTypeSettings.getSettings(livingEntity.getType());
        if (settings != null) {
            EntityTypeSettings.CustomDrops dropsSettings = settings.getCustomDrops();
            if (dropsSettings != null) {
                List<CustomDrop> customDrops = dropsSettings.getItems();

                for (int i = 0; i < customDrops.size(); i++) {
                    CustomDrop drop = customDrops.get(ThreadLocalRandom.current().nextInt(0, customDrops.size()));
                    ItemStack item = drop.getDrop();
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        }

        return items;
    }
}
