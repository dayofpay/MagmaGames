package org.vdevs.plugins.magmagames.Events;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.vdevs.plugins.magmagames.MagmaGames;
import org.vdevs.plugins.magmagames.Utils.MessageHandler;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class QuizGame implements Listener {

    private final MagmaGames plugin;
    private final MessageHandler msg;
    private boolean active = false;
    private String correctAnswer = "";
    private List<String> rewards;
    private BukkitRunnable timeoutTask = null;

    public QuizGame(MagmaGames plugin) {
        this.plugin = plugin;
        this.msg = new MessageHandler(plugin);
    }

    public void startQuiz(String question, String answer) {
        if (active) {
            Bukkit.broadcastMessage(msg.format("already_active", "Quiz Game", null, 0));
            return;
        }
        correctAnswer = answer.trim().toLowerCase();
        rewards = plugin.getConfig().getStringList("quiz_event.rewards");
        active = true;

        Bukkit.broadcastMessage(msg.format("start", "Quiz Game", null, 0));
        Bukkit.broadcastMessage("§eВъпрос: §f" + question);
        Bukkit.broadcastMessage(msg.get("divider"));

        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (active) {
                    active = false;
                    Bukkit.broadcastMessage(msg.format("no_answer", "Quiz Game", null, 0));
                }
            }
        };
        timeoutTask.runTaskLater(plugin, 20 * plugin.getConfig().getInt("quiz_event.duration-seconds", 30));
    }

    public void startRandomQuestion() {
        List<Map<?, ?>> questions = plugin.getConfig().getMapList("quiz_event.questions");
        if (questions.isEmpty()) return;
        Map<?, ?> random = questions.get(new Random().nextInt(questions.size()));
        String q = random.get("question").toString();
        String a = random.get("answer").toString();
        startQuiz(q, a);
    }

    public void stopQuiz() {
        if (!active) return;
        active = false;
        if (timeoutTask != null) timeoutTask.cancel();
        Bukkit.broadcastMessage(msg.format("stop", "Quiz Game", null, 0));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!active) return;
        String message = e.getMessage().trim().toLowerCase();
        if (message.equalsIgnoreCase(correctAnswer)) {
            active = false;
            if (timeoutTask != null) timeoutTask.cancel();
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcastMessage(msg.format("player_won", "Quiz Game", e.getPlayer().getName(), 0));
                for (String cmd : rewards)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", e.getPlayer().getName()));
            });
        }
    }

    public boolean isActive() { return active; }
}
