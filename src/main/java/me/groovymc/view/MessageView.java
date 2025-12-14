package me.groovymc.view;

import me.groovymc.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;

public class MessageView {
    private static final String PREFIX = ChatUtils.color("&8[&6GroovyMC&8] &r");

    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatUtils.color(message));
    }

    public static void logError(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatUtils.color("&c[ERROR] " + message));
    }

    public static void logError(String message, Exception e) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatUtils.color("&c[ERROR] " + message), e.toString());
    }

    public static void logScriptError(String moduleName, Exception e) {
        MessageView.logError("&c---------------------------------------------");
        MessageView.logError("Module: '" + moduleName + "' failed to load/execute!");

        if (e instanceof MultipleCompilationErrorsException) {
            MessageView.logError("&eReason: &fSyntax Error");

            String msg = e.getMessage();
            if (msg.contains("startup failed:")) msg = msg.replace("startup failed:", "").trim();

            // msg = msg.replaceAll(".*\\.groovy: \\d+: ", "");

            MessageView.logError("&7Detail: " + msg);
        }
        else {
            MessageView.logError("&eError Type: &f" + e.getClass().getSimpleName());
            MessageView.logError("&eMessage: &f" + e.getMessage());

            boolean foundInScript = false;

            for (StackTraceElement element : e.getStackTrace()) {
                String fileName = element.getFileName();

                if (fileName != null && fileName.endsWith(".groovy")) {
                    MessageView.logError("&6Location: &e" + fileName + " &7-> &cLine " + element.getLineNumber());
                    foundInScript = true;
                    break;
                }
            }

            if (!foundInScript) {
                MessageView.logError("&7(Error source not found in script, check console for details)");
                e.printStackTrace();
            }
        }
        MessageView.logError("&c---------------------------------------------");
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