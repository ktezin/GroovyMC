package me.groovymc.view;

import me.groovymc.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class MessageView {
    private static final String PREFIX = ChatUtils.color("&8[&6GroovyMC&8] ");

    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color(message));
    }

    public static void logError(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color("&c[ERROR] " + message));
    }

    public static void logError(String message, Exception e) {
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color("&c[ERROR] " + message), e.toString());
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatUtils.color(message));
    }

    public static void sendSuccess(CommandSender sender, String message) {
        send(sender, "&a" + ChatUtils.color(message));
    }

    public static void sendError(CommandSender sender, String message) {
        send(sender, "&c" + ChatUtils.color(message));
    }

    public static void broadcast(String message) {
        Bukkit.broadcastMessage(PREFIX + ChatUtils.color(message));
    }
}