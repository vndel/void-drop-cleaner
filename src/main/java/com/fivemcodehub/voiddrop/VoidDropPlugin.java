package com.fivemcodehub.voiddrop;

import com.fivemcodehub.voiddrop.command.VoidDropCommand;
import com.fivemcodehub.voiddrop.sweep.ItemMerger;
import com.fivemcodehub.voiddrop.sweep.SweepScheduler;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ground-item sweeper.
 *
 * <p>Most "clear lag" plugins delete every dropped item on a timer and
 * announce a countdown. That is hostile to players mid-build and still leaves
 * the entity count spiking between sweeps. This merges stackable drops first,
 * which removes most of the entity pressure without destroying anything, and
 * only then removes what remains past its age limit.
 */
public final class VoidDropPlugin extends JavaPlugin {

    private SweepScheduler scheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ItemMerger merger = new ItemMerger(this);
        this.scheduler = new SweepScheduler(this, merger);
        this.scheduler.start();

        var cmd = getCommand("voiddrop");
        if (cmd != null) {
            VoidDropCommand executor = new VoidDropCommand(this, scheduler);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("VoidDropCleaner enabled (merge radius "
                + getConfig().getDouble("merge.radius", 2.5) + ")");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.shutdown();
    }
}
