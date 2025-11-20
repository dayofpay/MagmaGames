package org.vdevs.plugins.magmagames.Events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.vdevs.plugins.magmagames.MagmaGames;
import org.vdevs.plugins.magmagames.Utils.MessageHandler;

import java.util.*;

public class MineEvent implements Listener {

    private final MagmaGames plugin;
    private final MessageHandler msg;
    private boolean active = false;

    private List<String> blockList = new ArrayList<>();
    private List<String> rewardCommands = new ArrayList<>();
    private Map<Integer, List<String>> preCommands = new HashMap<>();
    private String startMessage;
    private String endMessage;
    private int durationSeconds;
    private BukkitRunnable stopTask;

    public MineEvent(MagmaGames plugin) {
        this.plugin = plugin;
        this.msg = new MessageHandler(plugin);
        loadConfigValues();
        startAutoScheduler();
    }


    private void startAutoScheduler() {
        int interval = plugin.getConfig().getInt("mine_event.interval-seconds", 1800);
        if (interval <= 0) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active && plugin.getConfig().getBoolean("events.mine_event", true)) {
                    manualStart(false);
                }
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L);
    }


    private void loadConfigValues() {
        FileConfiguration cfg = plugin.getConfig();

        blockList = cfg.getStringList("mine_event.blocks");
        rewardCommands = cfg.getStringList("mine_event.rewards");
        startMessage = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("mine_event.broadcast.start", "&aMine Event започна!"));
        endMessage = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("mine_event.broadcast.end", "&cMine Event приключи!"));
        durationSeconds = cfg.getInt("mine_event.duration-seconds", 300);

        ConfigurationSection section = cfg.getConfigurationSection("mine_event.pre-commands");
        preCommands.clear();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int sec = Integer.parseInt(key);
                    preCommands.put(sec, section.getStringList(key));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid pre-command key: " + key);
                }
            }
        }
    }


    public void manualStart(boolean force) {
        if (active) {
            Bukkit.broadcastMessage(msg.format("already_active", "Mine Event", null, 0));
            return;
        }

        if (force || preCommands.isEmpty()) {
            startEvent();
            return;
        }

        Bukkit.broadcastMessage(ChatColor.GRAY + "[Event] Започва обратно броене за Mine Event...");
        int maxDelay = preCommands.keySet().stream().max(Integer::compareTo).orElse(0);

        for (Map.Entry<Integer, List<String>> entry : preCommands.entrySet()) {
            int sec = entry.getKey();
            List<String> cmds = entry.getValue();

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (String cmd : cmds) {
                        String finalCmd = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cmd));
                        Bukkit.getScheduler().runTask(plugin, () ->
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd)
                        );
                    }
                }
            }.runTaskLater(plugin, (maxDelay - sec) * 20L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                startEvent();
            }
        }.runTaskLater(plugin, maxDelay * 20L);
    }


    public void manualStop() {
        stopEvent();
    }


    private void startEvent() {
        if (active) return;
        active = true;
        plugin.setMineEventEnabled(true);

        Bukkit.broadcastMessage(startMessage);

        stopTask = new BukkitRunnable() {
            @Override
            public void run() {
                stopEvent();
            }
        };
        stopTask.runTaskLater(plugin, durationSeconds * 20L);
    }


    private void stopEvent() {
        if (!active) return;
        active = false;
        plugin.setMineEventEnabled(false);
        if (stopTask != null) stopTask.cancel();
        Bukkit.broadcastMessage(endMessage);
    }


    @EventHandler
    public void onMine(BlockBreakEvent e) {
        if (!active || !plugin.isMineEventEnabled()) return;

        String type = e.getBlock().getType().name().toLowerCase();
        for (String allowed : blockList) {
            String normalized = allowed.replace("minecraft:", "").toLowerCase();
            if (type.equals(normalized)) {
                executeRewards(e.getPlayer(), type);
                break;
            }
        }
    }


    private void executeRewards(Player player, String blockName) {
        for (String cmd : rewardCommands) {
            String finalCmd = cmd.replace("%player%", player.getName()).replace("%block%", blockName);
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd)
            );
        }
    }

    public boolean isActive() {
        return active;
    }
}
