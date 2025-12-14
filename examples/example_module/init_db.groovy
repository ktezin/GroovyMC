package example_module

import me.groovymc.api.ScriptAPI
import groovy.transform.BaseScript
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent

@BaseScript ScriptAPI base

// --- DATABASE SCHEMA DEFINITION ---
// Defines the 'stats' table structure.
// If the table does not exist, it will be created with these columns.
db.define("stats")
        .uuidKey("uuid")       // Primary Key: Player UUID
        .string("name", 16)    // Column: Player Name (Max 16 chars)
        .integer("kills")      // Column: Kill count
        .integer("deaths")     // Column: Death count
        .create()              // Finalizes and builds the table

command("stats") { sender, args ->
    // Ensure the sender is a player (console cannot have stats)
    if (!(sender instanceof Player)) return
    def p = (Player) sender

    // Using 'async' to run database queries in a separate thread.
    // This prevents the main server thread from freezing (lag) while fetching data.
    async {
        // Query: Select the first record where 'uuid' matches the player's ID.
        def data = db.stats.where("uuid").is(p.uniqueId.toString()).first()

        // Display the fetched statistics to the player
        message(sender, "&ePlayer Stats:")
        message(sender, "&rKills: &e${data.kills}")
        message(sender, "&rDeaths: &e${data.deaths}")
    }
}

// Initializes player data when they join the server.
onEvent(PlayerJoinEvent) { e ->
    def p = e.player

    async {
        // Check if the player already exists in the database
        def data = db.stats.where("uuid").is(p.uniqueId.toString()).first()

        if (!data) {
            // If data is null (player is new), insert a new record with default values.
            db.stats.add(uuid: p.uniqueId.toString(), name: p.name, kills: 0, deaths: 0)
        }
    }
}

// Updates kills and deaths when a player dies.
onEvent(PlayerDeathEvent) { e ->
    def p = e.player

    // Update Victim's Deaths
    async {
        // Create a query for the victim
        def data = db.stats.where("uuid").is(p.uniqueId)

        // Get current death count
        def deaths = data.first().deaths

        // Modify the record: Increment deaths by 1
        data.modify(deaths: deaths + 1)
    }

    // Update Killer's Kills (if the killer is a player)
    if (p.getKiller() != null) {
        def killer = p.getKiller()
        async {
            // Create a query for the killer
            def data = db.stats.where("uuid").is(killer.uniqueId)

            // Get current kill count
            def kills = data.first().kills

            // Updating the existing data: Increment kills by 1
            data.modify(kills: kills + 1)
        }
    }
}