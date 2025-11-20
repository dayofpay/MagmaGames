package org.vdevs.plugins.magmagames.Events;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.vdevs.plugins.magmagames.MagmaGames;
import org.vdevs.plugins.magmagames.Utils.MessageHandler;
import java.util.List;
import java.util.Random;

public class MathEvent implements Listener {

    private final MagmaGames plugin;
    private final MessageHandler msg;
    private boolean active = false;
    private int answer;
    private List<String> rewards;
    private BukkitRunnable finishTask = null;

    public MathEvent(MagmaGames plugin) {
        this.plugin = plugin;
        this.msg = new MessageHandler(plugin);
    }

    public void manualStart() {
        if (active) {
            Bukkit.broadcastMessage(msg.format("already_active", "Math Event", null, 0));
            return;
        }
        startMathEvent();
    }

    public void manualStop() {
        if (!active) return;
        active = false;
        plugin.setMathEventEnabled(false);
        if (finishTask != null) finishTask.cancel();
        Bukkit.broadcastMessage(msg.format("stop", "Math Event", null, 0));
    }

    public void startMathEvent() {
        if (active) return;

        Random r = new Random();
        int a = r.nextInt(50) + 1;
        int b = r.nextInt(50) + 1;
        answer = a + b;
        rewards = plugin.getConfig().getStringList("math_event.rewards");

        active = true;
        plugin.setMathEventEnabled(true);

        Bukkit.broadcastMessage(msg.format("start", "Math Event", null, 0));
        Bukkit.broadcastMessage(msg.get("divider"));
        Bukkit.broadcastMessage("§bКолко е §f" + a + " + " + b + "§b?");
        Bukkit.broadcastMessage(msg.get("divider"));

        finishTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (active) {
                    active = false;
                    plugin.setMathEventEnabled(false);
                    Bukkit.broadcastMessage(msg.format("no_answer", "Math Event", null, 0));
                }
            }
        };
        finishTask.runTaskLater(plugin, 20L * plugin.getConfig().getInt("math_event.duration-seconds", 30));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!active) return;
        try {
            int resp = Integer.parseInt(e.getMessage().trim());
            if (resp == answer) {
                active = false;
                plugin.setMathEventEnabled(false);
                if (finishTask != null) finishTask.cancel();
                Bukkit.broadcastMessage(msg.format("correct_answer", "Math Event", e.getPlayer().getName(), 0));
                for (String cmd : rewards)
                    Bukkit.getScheduler().runTask(plugin, () ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    cmd.replace("%player%", e.getPlayer().getName())));
            }
        } catch (NumberFormatException ignored) {}
    }

    public boolean isActive() { return active; }
}
