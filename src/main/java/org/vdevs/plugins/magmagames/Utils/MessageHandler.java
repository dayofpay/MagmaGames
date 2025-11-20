package org.vdevs.plugins.magmagames.Utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.vdevs.plugins.magmagames.MagmaGames;

public class MessageHandler {
    private final MagmaGames plugin;

    public MessageHandler(MagmaGames plugin) {
        this.plugin = plugin;
    }


    public String get(String key) {
        FileConfiguration config = plugin.getConfig();
        String prefix = color(config.getString("messages.prefix", "&7[Event]&f "));
        String fullPath = "messages." + key;
        String msg = config.getString(fullPath);

        if (msg == null) {
            plugin.getLogger().warning("[Event] Missing message: " + key);
            msg = "&cMissing message: " + key;
        }

        return color(prefix + msg);
    }


    public String format(String key, String eventName, String playerName, int seconds) {
        String msg = get(key)
                .replace("%event_name%", eventName != null ? eventName : "")
                .replace("%player%", playerName != null ? playerName : "")
                .replace("%seconds%", String.valueOf(seconds));
        return color(msg);
    }


    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
