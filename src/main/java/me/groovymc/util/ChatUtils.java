package me.groovymc.util;

import org.bukkit.ChatColor;
import java.util.List;
import java.util.stream.Collectors;

public class ChatUtils {

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String color(Object obj) {
        if (obj == null) return "";
        return color(obj.toString());
    }

    public static List<String> color(List<?> list) {
        return list.stream()
                .map(ChatUtils::color)
                .collect(Collectors.toList());
    }
}