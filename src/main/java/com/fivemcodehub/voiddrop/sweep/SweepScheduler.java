package com.fivemcodehub.voiddrop.sweep;

import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Drives merge and sweep passes, budgeted against tick health.
 *
 * <p>The sweep yields when MSPT is already high. Running a full entity scan
 * during a lag spike makes the spike worse, which is the opposite of the
 * plugin's purpose.
 */
public final class SweepScheduler {

    private final JavaPlugin plugin;
    private final ItemMerger merger;

    private final AtomicLong merged = new AtomicLong();
    private final AtomicLong removed = new AtomicLong();
    private final AtomicLong passes = new AtomicLong();

    private BukkitTask task;

    public SweepScheduler(JavaPlugin plugin, ItemMerger merger) {
        this.plugin = plugin;
        this.merger = merger;
    }

    public void start() {
        long interval = Math.max(100L, plugin.getConfig()
                .getLong("sweep.interval-ticks", 1200L));
        this.task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::runPass, interval, interval);
    }

    /** One merge-then-sweep pass. Returns entities removed. */
    public int runPass() {
        double abortAbove = plugin.getConfig().getDouble("sweep.abort-above-mspt", 48.0);
        if (plugin.getServer().getAverageTickTime() > abortAbove) {
            plugin.getLogger().fine("Sweep skipped: server already over tick budget");
            return 0;
        }

        long minimumAge = plugin.getConfig().getLong("sweep.minimum-age-ticks", 1200L);
        int entityThreshold = plugin.getConfig().getInt("sweep.entity-threshold", 300);

        int mergedTotal = 0;
        int removedTotal = 0;

        for (World world : plugin.getServer().getWorlds()) {
            if (plugin.getConfig().getStringList("sweep.excluded-worlds")
                    .contains(world.getName())) {
                continue;
            }

            int before = world.getEntitiesByClass(Item.class).size();

            if (plugin.getConfig().getBoolean("merge.enabled", true)) {
                mergedTotal += merger.mergeWorld(world);
            }

            // Removal only engages once the count is genuinely high; below the
            // threshold, merging alone has already dealt with the pressure.
            if (before < entityThreshold) continue;

            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (!merger.isSweepable(item, minimumAge)) continue;
                item.remove();
                removedTotal++;
            }
        }

        merged.addAndGet(mergedTotal);
        removed.addAndGet(removedTotal);
        passes.incrementAndGet();

        if ((mergedTotal > 0 || removedTotal > 0)
                && plugin.getConfig().getBoolean("sweep.log-passes", true)) {
            plugin.getLogger().info("Sweep: merged " + mergedTotal
                    + ", removed " + removedTotal);
        }
        return removedTotal;
    }

    public long mergedTotal() {
        return merged.get();
    }

    public long removedTotal() {
        return removed.get();
    }

    public long passCount() {
        return passes.get();
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }
}
