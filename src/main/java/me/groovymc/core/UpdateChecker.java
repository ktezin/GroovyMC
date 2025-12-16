package me.groovymc.core;

import me.groovymc.view.MessageView;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String repoUser;
    private final String repoName;

    public UpdateChecker(JavaPlugin plugin, String repoUser, String repoName) {
        this.plugin = plugin;
        this.repoUser = repoUser;
        this.repoName = repoName;
    }

    public void check(final Consumer<String> onUpdateFound) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create("https://api.github.com/repos/" + repoUser + "/" + repoName + "/releases/latest").toURL();

                try (InputStream inputStream = url.openStream(); Scanner scanner = new Scanner(inputStream)) {
                    if (scanner.hasNext()) {
                        String response = scanner.useDelimiter("\\A").next();

                        String versionMarker = "\"tag_name\":\"";
                        int index = response.indexOf(versionMarker);

                        if (index != -1) {
                            int start = index + versionMarker.length();
                            int end = response.indexOf("\"", start);
                            String latestVersion = response.substring(start, end); // Örn: v0.1.1

                            String currentVersion = "v" + plugin.getPluginMeta().getVersion().replace("v", "");

                            if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                                Bukkit.getScheduler().runTask(plugin, () -> onUpdateFound.accept(latestVersion));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                MessageView.logError("Failed to check for updates: " + e.getMessage());
            }
        });
    }
}