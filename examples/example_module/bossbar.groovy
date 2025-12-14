package example_module

import me.groovymc.api.ScriptAPI
import groovy.transform.BaseScript
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerQuitEvent

@BaseScript ScriptAPI base

// We store the current BossBar instance globally to manage it later.
// Initialized as null.
customBar = null

// Starts a countdown timer shown in a BossBar at the top of the screen.
command("custombar") { sender, args ->
    // If a bar is already active, don't start a new one.
    if (customBar != null) return;

    // --- CREATE BOSSBAR ---
    // Syntax: bossbar(title, color, style, progress)
    // Valid colors: PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    // Valid styles: SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
    customBar = bossBar("&c&lCountdown", "RED", "SEGMENTED_6", 1.0)

    // Add all currently online players to the bar so they can see it.
    Bukkit.onlinePlayers.each { p ->
        customBar.addPlayer(p)
    }

    // --- ANIMATION LOOP ---
    def count = 100

    // Repeat task:
    // - Initial Delay: 0 ticks (starts immediately)
    // - Period: 1 tick (runs 20 times per second for smooth animation)
    repeat(0, 1) {
        if (count > 0) {
            count--

            // Update Progress: Must be a float between 0.0 (empty) and 1.0 (full).
            customBar.progress = count / 100.0

            // Update Title: Shows the remaining count dynamically.
            customBar.title = (color("&c&lCountdown: &e${count}"))
        } else {
            // --- FINISH LOGIC ---
            // When countdown hits 0:

            // 1. Remove the bar from all players' screens.
            customBar?.removeAll()

            // 2. Reset the global variable to allow the command to run again later.
            customBar = null

            // 3. Stop the scheduler loop.
            cancel()
        }
    }
}

// Clean up individual players if they leave while the bar is active.
onEvent(PlayerQuitEvent) { e ->
    if (customBar) customBar.removePlayer(e.player)
}