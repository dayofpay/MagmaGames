package org.vdevs.plugins.magmagames;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.vdevs.plugins.magmagames.Commands.StartGame;
import org.vdevs.plugins.magmagames.Commands.TreasureCommand;
import org.vdevs.plugins.magmagames.Commands.TreasureCompassCommand;
import org.vdevs.plugins.magmagames.Events.*;

public final class MagmaGames extends JavaPlugin {

    private boolean mineEventEnabled = false;
    private boolean killEventEnabled = false;
    private boolean mathEventEnabled = false;

    private MineEvent mineEvent;
    private KillEvent killEvent;
    private MathEvent mathEvent;
    private QuizGame quizGame;
    private TreasureHuntEvent treasureHunt;

    private BukkitRunnable autoSchedulerTask;

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&8[&cMagma&6Games&8]&7 ");

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        saveDefaultConfig();
        reloadConfig();

        sendConsole("&7------------------------------------");
        sendConsole("&c☯ Loading &eMagmaGames &7plugin...");
        sendConsole("&7------------------------------------");

        try {
            mineEvent = new MineEvent(this);
            killEvent = new KillEvent(this);
            mathEvent = new MathEvent(this);
            quizGame = new QuizGame(this);
            treasureHunt = new TreasureHuntEvent(this);


            registerListeners(mineEvent, killEvent, mathEvent, quizGame, treasureHunt);


            if (getCommand("hostgame") != null)
                getCommand("hostgame").setExecutor(new StartGame(this));

            if (getCommand("activechests") != null)
                getCommand("activechests").setExecutor(new TreasureCommand(this, treasureHunt));

            if (getCommand("treasurecompass") != null)
                getCommand("treasurecompass").setExecutor(new TreasureCompassCommand(this, treasureHunt));


            startAutoScheduler();

            double loadTime = (System.currentTimeMillis() - start) / 1000.0;
            sendConsole("&a☯ Successfully loaded all events &7(in &e" + loadTime + "s&7)");
        } catch (Exception e) {
            sendConsole("&c⚠ Error loading MagmaGames: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        sendConsole("&7------------------------------------");
    }

    @Override
    public void onDisable() {
        sendConsole("&c☯ Disabling MagmaGames...");

        Bukkit.getScheduler().cancelTasks(this);

        if (autoSchedulerTask != null) {
            autoSchedulerTask.cancel();
            autoSchedulerTask = null;
        }

        // ram cleanup, prevent thread from memory leak
        TreasureHuntEvent.getActiveChests().clear();


        if (mineEvent != null) {
            if (mineEvent.isActive()) mineEvent.manualStop();
            mineEvent = null;
        }

        if (killEvent != null) {
            if (killEvent.isActive()) killEvent.manualStop();
            killEvent = null;
        }

        if (mathEvent != null) {
            if (mathEvent.isActive()) mathEvent.manualStop();
            mathEvent = null;
        }

        if (quizGame != null) {
            if (quizGame.isActive()) quizGame.stopQuiz();
            quizGame = null;
        }

        if (treasureHunt != null) {
            if (treasureHunt.isActive()) treasureHunt.manualStop(false, null);
            treasureHunt = null;
        }
        Hologram decentHologram = DHAPI.getHologram("treasure_countdown");
        if (decentHologram != null)
            DHAPI.removeHologram("treasure_countdown");
        sendConsole("&a☯ All scheduled tasks cancelled and memory freed.");
        sendConsole("&cMagmaGames plugin disabled.");
    }


    private void startAutoScheduler() {
        autoSchedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnabled()) {
                    cancel();
                    return;
                }

                if (mathEvent != null && !mathEvent.isActive()) {
                    mathEvent.manualStart();
                }

                // Стартира Quiz след 60 секунди
                Bukkit.getScheduler().runTaskLater(MagmaGames.this, () -> {
                    if (quizGame != null && !quizGame.isActive() && isEnabled()) {
                        quizGame.startRandomQuestion();
                    }
                }, 20L * 60);
            }
        };
        autoSchedulerTask.runTaskTimer(this, 20L * 60, 20L * 3600);
    }

    private void registerListeners(Object... listeners) {
        for (Object l : listeners) {
            if (l instanceof org.bukkit.event.Listener)
                Bukkit.getPluginManager().registerEvents((org.bukkit.event.Listener) l, this);
        }
    }

    // getters i setters
    public boolean isMineEventEnabled() { return mineEventEnabled; }
    public void setMineEventEnabled(boolean b) { this.mineEventEnabled = b; }

    public boolean isKillEventEnabled() { return killEventEnabled; }
    public void setKillEventEnabled(boolean b) { this.killEventEnabled = b; }

    public boolean isMathEventEnabled() { return mathEventEnabled; }
    public void setMathEventEnabled(boolean b) { this.mathEventEnabled = b; }

    public MineEvent getMineEvent() { return mineEvent; }
    public KillEvent getKillEvent() { return killEvent; }
    public MathEvent getMathEvent() { return mathEvent; }
    public QuizGame getQuizGame() { return quizGame; }
    public TreasureHuntEvent getTreasureHunt() { return treasureHunt; }

    private void sendConsole(String msg) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', msg));
    }
}
