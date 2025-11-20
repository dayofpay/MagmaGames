package org.vdevs.plugins.magmagames.Commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.vdevs.plugins.magmagames.Events.TreasureHuntEvent;
import org.vdevs.plugins.magmagames.MagmaGames;

public class TreasureCompassCommand implements CommandExecutor {

    private final MagmaGames plugin;
    private final TreasureHuntEvent treasureEvent;

    public TreasureCompassCommand(MagmaGames plugin, TreasureHuntEvent treasureEvent) {
        this.plugin = plugin;
        this.treasureEvent = treasureEvent;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Тази команда е само за играчи.");
            return true;
        }

        Player player = (Player) sender;


        if (!treasureEvent.isActive()) {
            player.sendMessage(ChatColor.RED + "В момента няма активно съкровище!");
            return true;
        }

        Location loc = treasureEvent.getChestLocation();
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "⚠Локацията на съкровището не е достъпна.");
            return true;
        }


        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Treasure Compass");
        meta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Този компас сочи към съкровището.",
                ChatColor.DARK_GRAY + "(Следвай стрелката!)"
        ));
        compass.setItemMeta(meta);

        player.getInventory().addItem(compass);
        player.setCompassTarget(loc);

        player.sendMessage(ChatColor.YELLOW + "Компасът ти сочи към " +
                ChatColor.GOLD + "съкровището!");
        return true;
    }
}
