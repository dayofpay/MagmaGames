package org.vdevs.plugins.magmagames.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.vdevs.plugins.magmagames.Events.TreasureHuntEvent;
import org.vdevs.plugins.magmagames.MagmaGames;

public class TreasureCommand implements CommandExecutor {

    private final MagmaGames plugin;
    private final TreasureHuntEvent treasureEvent;

    public TreasureCommand(MagmaGames plugin, TreasureHuntEvent treasureEvent) {
        this.plugin = plugin;
        this.treasureEvent = treasureEvent;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("magma.admin")) {
            sender.sendMessage(ChatColor.RED + "Нямаш права за тази команда.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Активни Treasure Chests:");
        if (TreasureHuntEvent.getActiveChests().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Няма активни честове в момента.");
        } else {
            for (Location loc : TreasureHuntEvent.getActiveChests()) {
                sender.sendMessage(ChatColor.YELLOW + "• " + loc.getWorld().getName() +
                        " x:" + loc.getBlockX() +
                        " y:" + loc.getBlockY() +
                        " z:" + loc.getBlockZ());
            }
        }
        return true;
    }
}
