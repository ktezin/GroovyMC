package me.groovymc.features.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Map;

public class CommandRegistry {
    private CommandMap commandMap;
    private final String pluginName;
    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pluginName = plugin.getName();
        setupCommandMap();
    }

    private void setupCommandMap() {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            this.commandMap = (CommandMap) f.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register(Command command) {
        if (commandMap == null) return;

        try {
            commandMap.register(pluginName, command);
            syncCommands();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregister(Command command) {
        if (commandMap == null) return;

        try {
            command.unregister(commandMap);

            Map<String, Command> knownCommands = null;

            try {
                knownCommands = (Map<String, Command>) commandMap.getClass().getMethod("getKnownCommands").invoke(commandMap);
            } catch (Exception e) {
                try {
                    Field f = commandMap.getClass().getDeclaredField("knownCommands");
                    f.setAccessible(true);
                    knownCommands = (Map<String, Command>) f.get(commandMap);
                } catch (Exception ignored) {}
            }

            if (knownCommands != null) {
                knownCommands.remove(command.getName());
                knownCommands.remove(pluginName.toLowerCase() + ":" + command.getName());
                knownCommands.values().removeIf(cmd -> cmd == command);
            }

            syncCommands();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncCommands() {
        if (!plugin.isEnabled()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }
        });
    }
}