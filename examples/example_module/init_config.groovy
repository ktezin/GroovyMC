package example_module

import me.groovymc.api.ScriptAPI
import groovy.transform.BaseScript
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent

@BaseScript ScriptAPI base

// --- CONFIGURATION DEFAULTS ---
// Sets default values for the config file. These are used if the keys don't exist yet.
config.setDefault('prefix', "&eExample &r->")      // String value
config.setDefault('spawn', [:])                     // Map (empty object) for location storage
config.setDefault('settings.wait', 20)              // Integer (Delay in ticks, 20 ticks = 1 sec)
config.setDefault('themes', ["Dragon", "Castle", "Spaceship", "Fast Food", "Pikachu"]) // List
saveConfig() // Writes changes to disk immediately


// Saves the player's current location as the spawn point in the config.
command("setspawn") { sender, args ->
    if (!(sender instanceof Player)) return

    def p = (Player) sender

    // Serializes the location object into a Map and assigns it to 'spawn' in config.
    config.spawn = serializeLoc(p.location)

    // Persist changes to the file.
    saveConfig()

    // Retrieve 'prefix' from config and send confirmation.
    message(p, "${config.prefix} &aSpawn has been set.")
}

// Picks a random theme from the config list and displays it after a delay.
command("randomtheme") { sender, args ->
    // Access the list from config
    def themes = config.themes
    // Select a random element
    def selected = themes[new Random().nextInt(themes.size())]

    // Scheduler: Waits for 'settings.wait' ticks (defined in config) before running.
    after(config.settings.wait) {
        message(sender, "${config.prefix} &aYour new theme is ${selected}")
    }
}

// Teleports the player to the spawn location defined in the config.
onEvent(PlayerJoinEvent) { e ->
    def data = config.spawn

    // Safety check: Ensure spawn data exists and is not empty.
    if (data == null || data.isEmpty()) return

    // Convert the Map back into a Bukkit Location object.
    def spawn = deserializeLoc(data)

    // Safety check: Ensure the world in the config actually exists.
    if (spawn == null) return

    e.player.teleport(spawn)
}

// --- HELPER CLOSURES (SERIALIZATION) ---

// Helper to convert a Location object into a simpler Map format for Config/JSON storage.
serializeLoc = { Location loc ->
    return [world: loc.world.name, x: loc.x, y: loc.y, z: loc.z, yaw: loc.yaw, pitch: loc.pitch]
}

// Helper to reconstruct a Location object from the stored Map.
deserializeLoc = { map ->
    // Returns null if the world is invalid/unloaded.
    if (Bukkit.getWorld(map.world) == null) return null

    return new Location(
            Bukkit.getWorld(map.world),
            map.x,
            map.y,
            map.z,
            (float)map.yaw,
            (float)map.pitch
    )
}