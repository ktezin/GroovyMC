package me.groovymc.view;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class MessageView {
    private final JavaPlugin plugin;
    private final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GOLD + "GroovyMC" + ChatColor.GRAY + "] ";

    public MessageView(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(String message) {
        plugin.getLogger().info(ChatColor.stripColor(message));
    }

    public void logError(String message, Exception e) {
        plugin.getLogger().log(Level.SEVERE, message, e);
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendSuccess(CommandSender sender, String message) {
        send(sender, ChatColor.GREEN + message);
    }

    public void sendError(CommandSender sender, String message) {
        send(sender, ChatColor.RED + message);
    }
}