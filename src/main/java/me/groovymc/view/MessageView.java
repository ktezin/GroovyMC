package me.groovymc.view;

import me.groovymc.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class MessageView {
    private static JavaPlugin plugin;
    private static final String PREFIX = ChatUtils.color("&8[&6GroovyMC&8] ");

    public static void init(JavaPlugin pl) {
        plugin = pl;
    }

    public static void log(String message) {
        if (plugin != null) {
            plugin.getLogger().info(ChatUtils.color(message));
        }
    }

    public static void logError(String message) {
        if (plugin != null) {
            plugin.getLogger().log(Level.SEVERE, message);
        }
    }

    public static void logError(String message, Exception e) {
        if (plugin != null) {
            plugin.getLogger().log(Level.SEVERE, message, e);
        }
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatUtils.color(message));
    }

    public static void sendSuccess(CommandSender sender, String message) {
        send(sender, "&a" + message);
    }

    public static void sendError(CommandSender sender, String message) {
        send(sender, "&c" + message);
    }

    public static void broadcast(String message) {
        Bukkit.broadcastMessage(PREFIX + ChatUtils.color(message));
    }
}