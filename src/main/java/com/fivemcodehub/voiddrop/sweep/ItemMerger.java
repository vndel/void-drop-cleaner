package com.fivemcodehub.voiddrop.sweep;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges nearby identical item stacks into one entity.
 *
 * <p>Two stacks are mergeable only when {@link ItemStack#isSimilar} agrees,
 * which compares type plus full metadata. Comparing by material alone would
 * silently destroy enchantments, custom names and durability, turning a
 * performance feature into item loss.
 */
public final class ItemMerger {

    private final JavaPlugin plugin;

    public ItemMerger(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Merges items in one world.
     *
     * @return the number of entities eliminated by merging
     */
    public int mergeWorld(World world) {
        double radius = plugin.getConfig().getDouble("merge.radius", 2.5);
        double radiusSquared = radius * radius;
        boolean respectMaxStack = plugin.getConfig().getBoolean("merge.respect-max-stack", true);

        List<Item> items = new ArrayList<>(world.getEntitiesByClass(Item.class));
        int eliminated = 0;

        for (int i = 0; i < items.size(); i++) {
            Item primary = items.get(i);
            if (primary.isDead() || !primary.isValid()) continue;
            if (isProtected(primary)) continue;

            ItemStack primaryStack = primary.getItemStack();
            Location primaryLocation = primary.getLocation();

            for (int j = i + 1; j < items.size(); j++) {
                Item candidate = items.get(j);
                if (candidate.isDead() || !candidate.isValid()) continue;
                if (isProtected(candidate)) continue;

                // Cheap spatial reject before the metadata comparison.
                if (primaryLocation.distanceSquared(candidate.getLocation()) > radiusSquared) {
                    continue;
                }

                ItemStack candidateStack = candidate.getItemStack();
                if (!primaryStack.isSimilar(candidateStack)) continue;

                int combined = primaryStack.getAmount() + candidateStack.getAmount();
                int ceiling = respectMaxStack
                        ? primaryStack.getMaxStackSize()
                        : Integer.MAX_VALUE;

                if (combined <= ceiling) {
                    primaryStack.setAmount(combined);
                    primary.setItemStack(primaryStack);
                    candidate.remove();
                    eliminated++;
                } else if (primaryStack.getAmount() < ceiling) {
                    // Partial merge: fill the primary, leave the remainder behind.
                    int moved = ceiling - primaryStack.getAmount();
                    primaryStack.setAmount(ceiling);
                    primary.setItemStack(primaryStack);
                    candidateStack.setAmount(candidateStack.getAmount() - moved);
                    candidate.setItemStack(candidateStack);
                }
            }
        }
        return eliminated;
    }

    /** Items a sweep must never touch. */
    private boolean isProtected(Item item) {
        if (plugin.getConfig().getBoolean("protect.named-items", true)
                && item.getItemStack().hasItemMeta()
                && item.getItemStack().getItemMeta() != null
                && item.getItemStack().getItemMeta().hasDisplayName()) {
            return true;
        }
        if (plugin.getConfig().getBoolean("protect.enchanted-items", true)
                && !item.getItemStack().getEnchantments().isEmpty()) {
            return true;
        }
        // Set by other plugins to opt an entity out of cleanup.
        return item.getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "protected"),
                org.bukkit.persistence.PersistentDataType.BYTE);
    }

    public boolean isSweepable(Item item, long minimumAgeTicks) {
        if (isProtected(item)) return false;

        List<String> excluded = plugin.getConfig().getStringList("protect.materials");
        if (excluded.contains(item.getItemStack().getType().name())) return false;

        return item.getTicksLived() >= minimumAgeTicks;
    }
}
