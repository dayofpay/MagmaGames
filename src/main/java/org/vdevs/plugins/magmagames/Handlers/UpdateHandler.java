package org.vdevs.plugins.magmagames.Handlers;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.vdevs.plugins.magmagames.MagmaGames;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class UpdateHandler {
    private final MagmaGames plugin;
    public UpdateHandler(final MagmaGames plugin) {
        this.plugin = plugin;
    }
    public void checkConfigUpdates() {
        plugin.saveDefaultConfig();
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        try {
            ConfigUpdater.update(plugin, "config.yml", configFile, Arrays.asList());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}