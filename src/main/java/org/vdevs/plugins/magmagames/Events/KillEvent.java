package org.vdevs.plugins.magmagames.Events;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.vdevs.plugins.magmagames.MagmaGames;
import org.vdevs.plugins.magmagames.Utils.MessageHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KillEvent implements Listener {

    private final MagmaGames plugin;
    private final MessageHandler msg;
    private boolean active = false;

    private String targetType;
    private int goal;
    private List<String> rewards;
    private int durationSeconds;
    private Map<UUID, Integer> kills = new HashMap<>();
    private Integer stopTaskId = null;

    public KillEvent(MagmaGames plugin) {
        this.plugin = plugin;
        this.msg = new MessageHandler(plugin);
        startAutoScheduler();
    }

    private void startAutoScheduler() {
        int interval = plugin.getConfig().getInt("kill_event.interval-seconds", 3600);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active && plugin.getConfig().getBoolean("events.kill_event", true)) {
                    manualStart(false);
                }
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L);
    }


    public void manualStart(boolean force) {
        if (active) {
            Bukkit.broadcastMessage(msg.format("already_active", "Kill Event", null, 0));
            return;
        }

        FileConfiguration cfg = plugin.getConfig();
        targetType = cfg.getString("kill_event.target", "players");
        goal = cfg.getInt("kill_event.goal", 10);
        rewards = cfg.getStringList("kill_event.rewards");
        durationSeconds = cfg.getInt("kill_event.duration-seconds", 600);

        startEvent();
    }

    public void manualStop() {
        stopEvent();
    }

    private void startEvent() {
        active = true;
        kills.clear();
        plugin.setKillEventEnabled(true);

        Bukkit.broadcastMessage(msg.format("start", "Kill Event", null, 0));
        Bukkit.broadcastMessage(msg.get("divider"));
        Bukkit.broadcastMessage("§7Първият, който направи §e" + goal + "§7 убийства, печели!");
        Bukkit.broadcastMessage(msg.get("divider"));

        // schedule stop
        new BukkitRunnable() {
            @Override
            public void run() {
                if (active) stopEvent();
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
    }

    private void stopEvent() {
        if (!active) return;
        active = false;
        plugin.setKillEventEnabled(false);
        kills.clear();
        Bukkit.broadcastMessage(msg.format("stop", "Kill Event", null, 0));
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (!active) return;
        if (!(e.getEntity().getKiller() instanceof Player)) return;

        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        // target filtering
        if (targetType.equalsIgnoreCase("players") && e.getEntityType() != EntityType.PLAYER) return;
        if (targetType.equalsIgnoreCase("mobs") && e.getEntityType() == EntityType.PLAYER) return;

        UUID id = killer.getUniqueId();
        int newKills = kills.getOrDefault(id, 0) + 1;
        kills.put(id, newKills);

        Bukkit.broadcastMessage(
                msg.format("progress", "Kill Event", killer.getName(), newKills)
                        .replace("%kills%", String.valueOf(newKills))
        );

        if (newKills >= goal) {
            Bukkit.broadcastMessage(msg.format("player_won", "Kill Event", killer.getName(), 0));
            for (String cmd : rewards) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", killer.getName()));
                });

            }
            stopEvent();
        }
    }

    // helper
    public boolean isActive() {
        return active;
    }
}
