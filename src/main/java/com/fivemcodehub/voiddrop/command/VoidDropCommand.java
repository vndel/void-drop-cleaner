package com.fivemcodehub.voiddrop.command;

import com.fivemcodehub.voiddrop.sweep.SweepScheduler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class VoidDropCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final SweepScheduler scheduler;

    public VoidDropCommand(JavaPlugin plugin, SweepScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "sweep" -> {
                int removedNow = scheduler.runPass();
                sender.sendMessage(MM.deserialize("<green>Pass complete: <n> removed.</green>",
                        Placeholder.unparsed("n", String.valueOf(removedNow))));
            }
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(MM.deserialize("<green>Configuration reloaded.</green>"));
            }
            default -> {
                int live = plugin.getServer().getWorlds().stream()
                        .mapToInt(w -> w.getEntitiesByClass(Item.class).size())
                        .sum();
                sender.sendMessage(MM.deserialize("<gray>──── VoidDropCleaner ────</gray>"));
                sender.sendMessage(MM.deserialize(
                        "<gray>Ground items now:</gray> <white><n></white>",
                        Placeholder.unparsed("n", String.valueOf(live))));
                sender.sendMessage(MM.deserialize(
                        "<gray>Merged total:</gray> <white><m></white>  "
                                + "<gray>Removed total:</gray> <white><r></white>",
                        Placeholder.unparsed("m", String.valueOf(scheduler.mergedTotal())),
                        Placeholder.unparsed("r", String.valueOf(scheduler.removedTotal()))));
                sender.sendMessage(MM.deserialize("<gray>Passes:</gray> <white><p></white>",
                        Placeholder.unparsed("p", String.valueOf(scheduler.passCount()))));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        return args.length == 1 ? List.of("status", "sweep", "reload") : List.of();
    }
}
