package me.groovymc.model;

import me.groovymc.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleSidebar {
    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, String> currentLines = new HashMap<>();

    public SimpleSidebar(Player player, String title) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("sidebar", "dummy", ChatUtils.color(title));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    public void setTitle(String title) {
        objective.setDisplayName(ChatUtils.color(title));
    }

    public void setLines(List<?> lines) {
        if (lines == null) return;

        int newSize = lines.size();
        int currentSize = currentLines.size();

        if (currentSize > newSize) {
            for (int i = newSize; i < currentSize; i++) {
                String entry = currentLines.get(i);
                if (entry != null) scoreboard.resetScores(entry);
                currentLines.remove(i);
            }
        }

        int score = 15;
        for (Object obj : lines) {
            String line = (obj == null) ? "" : obj.toString();
            setLine(score--, line);
        }
    }

    private void setLine(int score, String text) {
        text = ChatUtils.color(text);

        String oldEntry = getEntryByScore(score);
        if (oldEntry != null) {
            if (oldEntry.equals(text)) return;
            scoreboard.resetScores(oldEntry);
        }

        objective.getScore(text).setScore(score);
    }

    private String getEntryByScore(int score) {
        for (String entry : scoreboard.getEntries()) {
            if (objective.getScore(entry).getScore() == score) {
                return entry;
            }
        }
        return null;
    }

    public void delete() {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public static class SidebarConfig {
        public String title;
        public List<Object> lines;
    }
}

