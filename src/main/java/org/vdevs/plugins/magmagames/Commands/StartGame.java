package org.vdevs.plugins.magmagames.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.vdevs.plugins.magmagames.MagmaGames;

import java.util.HashMap;
import java.util.UUID;

public class StartGame implements CommandExecutor {

    private final MagmaGames plugin;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private static final long COMMAND_COOLDOWN_MS = 5000; // 5 sekundi

    public StartGame(MagmaGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /hostgame <event> <start|stop> [force|question|answer]"));
            sender.sendMessage(color("&7Примери:"));
            sender.sendMessage(color("&f/hostgame mineevent start"));
            sender.sendMessage(color("&f/hostgame killevent start"));
            sender.sendMessage(color("&f/hostgame mathevent start"));
            sender.sendMessage(color("&f/hostgame quiz start <въпрос> <отговор>"));
            sender.sendMessage(color("&f/hostgame treasure start"));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID id = player.getUniqueId();
            if (cooldowns.containsKey(id)) {
                long timeLeft = (cooldowns.get(id) + COMMAND_COOLDOWN_MS) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    player.sendMessage(color("&eИзчакай &6" + (timeLeft / 1000.0) + "s &eпреди да използваш пак командата."));
                    return true;
                }
            }
            cooldowns.put(id, System.currentTimeMillis());
        }

        String eventName = args[0].toLowerCase();
        String action = args[1].toLowerCase();
        boolean force = args.length > 2 && args[2].equalsIgnoreCase("force");

        switch (eventName) {
            case "mineevent":
                handleMineEvent(sender, action, force);
                break;

            case "killevent":
                handleKillEvent(sender, action);
                break;

            case "mathevent":
                handleMathEvent(sender, action);
                break;

            case "quiz":
                handleQuizEvent(sender, args);
                break;

            case "treasure":
            case "treasurehunt":
                handleTreasureHunt(sender, action);
                break;

            default:
                sender.sendMessage(color("&cНевалиден event!"));
                sender.sendMessage(color("&7Достъпни: &fmineevent, killevent, mathevent, quiz, treasurehunt"));
                break;
        }

        return true;
    }

    // ===========================================================
    // ⛏ Mine Event
    // ===========================================================
    private void handleMineEvent(CommandSender sender, String action, boolean force) {
        switch (action) {
            case "start":
                if (plugin.isMineEventEnabled()) {
                    sender.sendMessage(color("&cMineEvent вече е активен."));
                } else {
                    plugin.getMineEvent().manualStart(force);
                    sender.sendMessage(color("&aMineEvent стартира успешно " + (force ? "(force)" : "") + "!"));
                }
                break;

            case "stop":
                if (!plugin.isMineEventEnabled()) {
                    sender.sendMessage(color("&cMineEvent не е активен."));
                } else {
                    plugin.getMineEvent().manualStop();
                    sender.sendMessage(color("&cMineEvent беше спрян."));
                }
                break;

            default:
                sender.sendMessage(color("&cНевалидна подкоманда. Използвай start/stop"));
        }
    }

    // ===========================================================
    // ⚔ Kill Event
    // ===========================================================
    private void handleKillEvent(CommandSender sender, String action) {
        switch (action) {
            case "start":
                if (plugin.isKillEventEnabled()) {
                    sender.sendMessage(color("&cKillEvent вече е активен."));
                } else {
                    plugin.getKillEvent().manualStart(false);
                    plugin.setKillEventEnabled(true);
                    sender.sendMessage(color("&aKillEvent стартира успешно!"));
                }
                break;

            case "stop":
                if (!plugin.isKillEventEnabled()) {
                    sender.sendMessage(color("&cKillEvent не е активен."));
                } else {
                    plugin.getKillEvent().manualStop();
                    plugin.setKillEventEnabled(false);
                    sender.sendMessage(color("&cKillEvent беше спрян."));
                }
                break;

            default:
                sender.sendMessage(color("&cНевалидна подкоманда. Използвай start/stop"));
        }
    }

    // ===========================================================
    // 🧮 Math Event
    // ===========================================================
    private void handleMathEvent(CommandSender sender, String action) {
        switch (action) {
            case "start":
                if (plugin.isMathEventEnabled()) {
                    sender.sendMessage(color("&cMathEvent вече е активен."));
                } else {
                    plugin.getMathEvent().startMathEvent();
                    plugin.setMathEventEnabled(true);
                    sender.sendMessage(color("&aMathEvent стартира успешно!"));
                }
                break;

            case "stop":
                sender.sendMessage(color("&cMathEvent няма нужда да се спира — приключва автоматично."));
                break;

            default:
                sender.sendMessage(color("&cНевалидна подкоманда. Използвай start."));
        }
    }

    // ===========================================================
    // 🧠 Quiz Event
    // ===========================================================
    private void handleQuizEvent(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(color("&cUsage: /hostgame quiz start <въпрос> <отговор>"));
            return;
        }

        String action = args[1].toLowerCase();
        if (!action.equals("start")) {
            sender.sendMessage(color("&cНевалидна подкоманда. Използвай: start"));
            return;
        }

        String question = args[2];
        String answer = args[3];

        plugin.getQuizGame().startQuiz(question, answer);
        sender.sendMessage(color("&aQuiz стартира с въпрос: &f" + question));
    }

    // ===========================================================
    // 💎 Treasure Hunt Event
    // ===========================================================
    private void handleTreasureHunt(CommandSender sender, String action) {
        switch (action) {
            case "start":
                if (plugin.getTreasureHunt().isActive()) {
                    sender.sendMessage(color("&cTreasure Hunt вече е активен."));
                } else {
                    plugin.getTreasureHunt().manualStart();
                    sender.sendMessage(color("&aTreasure Hunt стартира успешно!"));
                }
                break;

            case "stop":
                if (!plugin.getTreasureHunt().isActive()) {
                    sender.sendMessage(color("&cTreasure Hunt не е активен."));
                } else {
                    plugin.getTreasureHunt().manualStop(false, null);
                    sender.sendMessage(color("&cTreasure Hunt беше спрян."));
                }
                break;

            default:
                sender.sendMessage(color("&cНевалидна подкоманда. Използвай start/stop"));
        }
    }

    // ===========================================================
    // 🎨 Utility
    // ===========================================================
    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
