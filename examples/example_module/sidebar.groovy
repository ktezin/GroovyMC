package example_module

import me.groovymc.api.ScriptAPI
import groovy.transform.BaseScript
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@BaseScript ScriptAPI base

onEvent(PlayerJoinEvent) { e ->
    def p = e.player
    // Initialize the sidebar immediately upon join.
    updateSidebar(p)
}

onEvent(PlayerQuitEvent) { e ->
    def p = e.player
    // Clean up sidebar data to prevent memory leaks or errors.
    removeSidebar(p)
}

// --- AUTO-UPDATER ---

// Global Scheduler: Updates the sidebar for ALL players every second.
// - 20: Interval in ticks (20 ticks = 1 second).
// - 0: Initial delay (starts immediately).
repeat(20, 0, {
    // Loop through all currently online players and refresh their board.
    // This ensures dynamic data (like Ping) stays accurate in real-time.
    Bukkit.onlinePlayers.each { p ->
        updateSidebar(p)
    }
})

// --- HELPER CLOSURE ---

// A reusable closure to define the sidebar layout and content.
// This avoids rewriting the same code in multiple places.
updateSidebar = { Player p ->
    // Uses the 'sidebar' builder from ScriptAPI.
    sidebar(p)  {
        title = "&e&lSIDEBAR" // The bold title at the top

        // List of lines to display.
        // Uses Groovy string interpolation (${...}) for dynamic values.
        lines = ["&7------------",
                 "&r",                // Empty line for spacing
                 "You: &e${p.name}",  // Shows player name
                 "Ping: &e${p.ping}ms", // Shows live latency
                 "&r&r",
                 "&r&7------------"]
    }
}