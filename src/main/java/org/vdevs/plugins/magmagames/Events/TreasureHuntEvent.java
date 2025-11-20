package org.vdevs.plugins.magmagames.Events;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.vdevs.plugins.magmagames.MagmaGames;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class TreasureHuntEvent implements Listener {

    private final MagmaGames plugin;
    private boolean active = false;
    private Location chestLocation;
    private Hologram hologram;
    private BukkitRunnable timeoutTask;
    private BukkitRunnable countdownTask;
    private BukkitRunnable chestHoloTicker;
    private List<String> rewards;

    private static final Set<Location> activeChests = new HashSet<>();


    private final int BORDER_RADIUS = 7500;
    private final int BORDER_CENTER_X = 3028;
    private final int BORDER_CENTER_Z = 13426;

    public static Set<Location> getActiveChests() {
        return activeChests;
    }

    public TreasureHuntEvent(MagmaGames plugin) {
        this.plugin = plugin;
        startAutoScheduler();
    }

    private void startAutoScheduler() {
        int interval = plugin.getConfig().getInt("treasure_event.interval-seconds", 3600);
        if (interval <= 0) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active && plugin.getConfig().getBoolean("treasure_event.enabled", true)) {
                    manualStart();
                }
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L);
    }

    public void manualStart() {
        if (active) {
            broadcastStyled(plugin.getConfig().getString("treasure_event.messages.already_active",
                    "&cTreasure Hunt вече е активен!"));
            return;
        }

        FileConfiguration cfg = plugin.getConfig();
        World world = Bukkit.getWorld(cfg.getString("treasure_event.world", "world"));
        if (world == null) return;


        Location validLocation = findValidLocation(world);
        if (validLocation == null) {
            Bukkit.getLogger().warning("[TreasureHunt] Не можа да се намери валидна позиция за съкровище в границите!");
            return;
        }

        chestLocation = validLocation;
        world.getBlockAt(chestLocation).setType(Material.CHEST);

        rewards = cfg.getStringList("treasure_event.rewards");

        Chest chest = (Chest) world.getBlockAt(chestLocation).getState();
        chest.getBlockInventory().clear();
        chest.update();

        active = true;
        activeChests.add(chestLocation);

        String startMsg = cfg.getString("treasure_event.messages.start",
                "&e[Event] &6Treasure Hunt започна! Намери скрития чест и спечели награди!");
        String coordMsg = cfg.getString("treasure_event.messages.coordinates",
                        "&7Координати (приблизително): &eX: %x% Y: %y% Z: %z%")
                .replace("%x%", String.valueOf((int)chestLocation.getX()))
                .replace("%y%", String.valueOf((int)chestLocation.getY()))
                .replace("%z%", String.valueOf((int)chestLocation.getZ()));

        broadcastStyled(startMsg);
        broadcastStyled(coordMsg);

        setupHologram();
        startTimeoutTimer();
        startCountdownHologram();
    }

    /**
     * Намира валидна позиция за съкровище в границите на world border
     */
    private Location findValidLocation(World world) {
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {

            int x = BORDER_CENTER_X + ThreadLocalRandom.current().nextInt(-BORDER_RADIUS, BORDER_RADIUS + 1);
            int z = BORDER_CENTER_Z + ThreadLocalRandom.current().nextInt(-BORDER_RADIUS, BORDER_RADIUS + 1);


            if (isInsideBorder(x, z)) {
                int y = world.getHighestBlockYAt(x, z);


                if (isSafeLocation(world, x, y, z)) {
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
            }
        }

        return null;
    }


    private boolean isInsideBorder(int x, int z) {
        double distanceX = Math.pow(x - BORDER_CENTER_X, 2);
        double distanceZ = Math.pow(z - BORDER_CENTER_Z, 2);
        double distance = Math.sqrt(distanceX + distanceZ);


        return distance <= (BORDER_RADIUS - 50);
    }


    private boolean isSafeLocation(World world, int x, int y, int z) {
        Location loc = new Location(world, x, y, z);

        // safe_entity_check
        Material blockType = loc.getBlock().getType();
        if (blockType == Material.WATER || blockType == Material.LAVA ||
                blockType == Material.STATIONARY_WATER || blockType == Material.STATIONARY_LAVA) {
            return false;
        }


        Material belowType = loc.clone().add(0, -1, 0).getBlock().getType();
        if (belowType == Material.AIR || belowType == Material.WATER || belowType == Material.LAVA ||
                belowType == Material.STATIONARY_WATER || belowType == Material.STATIONARY_LAVA) {
            return false;
        }


        Material above1 = loc.clone().add(0, 1, 0).getBlock().getType();
        Material above2 = loc.clone().add(0, 2, 0).getBlock().getType();

        if (above1 != Material.AIR || above2 != Material.AIR) {
            return false;
        }

        return true;
    }

    private void setupHologram() {
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) return;

        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("treasure_event.hologram.enabled", true)) return;

        hologram = DHAPI.createHologram("treasure_" + System.currentTimeMillis(),
                chestLocation.clone().add(0, 2, 0));

        List<String> lines = cfg.getStringList("treasure_event.hologram.lines");
        if (lines == null || lines.isEmpty()) {
            lines.add("&6&lСъкровище!");
            lines.add("&eОтвори ме, за да получиш награда!");
            lines.add("&7Ще изчезна след: &c%treasure_time%");
        }

        for (String line : lines) {
            DHAPI.addHologramLine(hologram, ChatColor.translateAlternateColorCodes('&', line));
        }

        startHologramTimer();
    }

    private void startHologramTimer() {
        FileConfiguration cfg = plugin.getConfig();
        int duration = cfg.getInt("treasure_event.duration-seconds", 600);

        chestHoloTicker = new BukkitRunnable() {
            int seconds = duration;

            @Override
            public void run() {
                if (!active || hologram == null || DHAPI.getHologram(hologram.getName()) == null) {
                    cancel();
                    return;
                }

                String formatted = formatTime(seconds);
                int size = DHAPI.getHologramPage(hologram, 0).size();

                for (int i = 0; i < size; i++) {
                    String current = DHAPI.getHologramLine(DHAPI.getHologramPage(hologram, 0), i).getText();
                    if (current.contains("%treasure_time%")) {
                        String updated = ChatColor.translateAlternateColorCodes('&',
                                current.replace("%treasure_time%", formatted));
                        DHAPI.setHologramLine(hologram, i, updated);
                    }
                }

                if (seconds <= 0) {
                    manualStop(false, null);
                    cancel();
                }
                seconds--;
            }
        };
        chestHoloTicker.runTaskTimer(plugin, 20L, 20L);
    }

    private void startTimeoutTimer() {
        FileConfiguration cfg = plugin.getConfig();
        int duration = cfg.getInt("treasure_event.duration-seconds", 600);

        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (active) manualStop(false, null);
            }
        };
        timeoutTask.runTaskLater(plugin, duration * 20L);
    }

    private void startCountdownHologram() {
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) return;

        FileConfiguration cfg = plugin.getConfig();
        World world = Bukkit.getWorld(cfg.getString("treasure_event.world", "world"));
        if (world == null) return;

        if (DHAPI.getHologram("treasure_countdown") != null) {
            DHAPI.removeHologram("treasure_countdown");
        }

        List<String> holoLines = cfg.getStringList("treasure_event.countdown-hologram.lines");
        if (holoLines == null || holoLines.isEmpty()) {
            holoLines = java.util.Arrays.asList(
                    "&eСледващ Treasure Event:",
                    "&6%treasure_time% &7оставащи"
            );
        }

        Hologram countdown = DHAPI.createHologram("treasure_countdown",
                world.getSpawnLocation().clone().add(0, 3, 0));

        for (String line : holoLines) {
            DHAPI.addHologramLine(countdown, ChatColor.translateAlternateColorCodes('&', line));
        }

        countdownTask = new BukkitRunnable() {
            int seconds = cfg.getInt("treasure_event.interval-seconds", 3600);

            @Override
            public void run() {
                Hologram holo = DHAPI.getHologram("treasure_countdown");
                if (holo == null) {
                    cancel();
                    return;
                }

                if (seconds <= 0) {
                    DHAPI.removeHologram("treasure_countdown");
                    cancel();
                    return;
                }

                String formatted = "§6" + formatTime(seconds) + " §7оставащи";
                int size = DHAPI.getHologramPage(holo, 0).size();

                for (int i = 0; i < size; i++) {
                    String current = DHAPI.getHologramLine(DHAPI.getHologramPage(holo, 0), i).getText();
                    if (current.contains("%treasure_time%")) {
                        String updated = ChatColor.translateAlternateColorCodes('&',
                                current.replace("%treasure_time%", formatted));
                        DHAPI.setHologramLine(holo, i, updated);
                    }
                }
                seconds--;
            }
        };
        countdownTask.runTaskTimer(plugin, 20L, 20L);
    }

    public void manualStop(boolean winnerFound, Player winner) {
        if (!active) return;
        active = false;


        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (chestHoloTicker != null) {
            chestHoloTicker.cancel();
            chestHoloTicker = null;
        }


        if (chestLocation != null) {
            activeChests.remove(chestLocation);
            if (chestLocation.getBlock().getType() == Material.CHEST) {
                chestLocation.getBlock().setType(Material.AIR);
            }
        }

        if (hologram != null) {
            if (DHAPI.getHologram(hologram.getName()) != null) {
                DHAPI.removeHologram(hologram.getName());
            }
            hologram = null;
        }


        String msgPath = winnerFound && winner != null
                ? "treasure_event.messages.found"
                : "treasure_event.messages.end";

        broadcastStyled(plugin.getConfig().getString(msgPath, "&7Event приключи.")
                .replace("%player%", winner != null ? winner.getName() : ""));



        chestLocation = null;
        rewards = null;
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent e) {
        if (!active || chestLocation == null) return;
        if (!(e.getInventory().getHolder() instanceof Chest)) return;

        Chest opened = (Chest) e.getInventory().getHolder();
        Location openLoc = opened.getLocation();
        if (openLoc == null || !openLoc.getWorld().equals(chestLocation.getWorld())) return;

        if (openLoc.getBlock().equals(chestLocation.getBlock())) {
            Player p = (Player) e.getPlayer();

            for (String cmd : rewards) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
            }

            manualStop(true, p);
        }
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent e) {
        if (!active || chestLocation == null) return;
        if (!e.getBlock().getWorld().equals(chestLocation.getWorld())) return;

        if (e.getBlock().getLocation().getBlock().equals(chestLocation.getBlock())) {
            manualStop(false, null);
        }
    }

    private void broadcastStyled(String raw) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&cMagma&6Events&8]&7 ");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix + raw));
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public boolean isActive() {
        return active;
    }

    public Location getChestLocation() {
        return chestLocation;
    }
}