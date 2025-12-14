// GroovyMC Module: package example_module
package example_module

import me.groovymc.api.ScriptAPI
import groovy.transform.BaseScript

// Applies the ScriptAPI base to allow usage of helper methods like 'log', 'command', etc.
@BaseScript ScriptAPI base

// --- MODULE IMPORTS ---
// Including external scripts to keep the project modular.
// These likely contain configuration, database logic, and specific features.
include("init_config.groovy")
include("init_db.groovy")
include("commands.groovy")
include("events.groovy")
include("bossbar.groovy")
include("sidebar.groovy")

// --- GLOBAL VARIABLES ---
// This variable is accessible throughout the script.
PREFIX = color("&8[&bExample&8] ")

// --- LIFECYCLE METHODS ---

// Executed when the module is enabled (loaded).
onEnable {
    log(PREFIX + "&aModule successfully loaded!")
}

// Executed when the module is disabled (unloaded).
onDisable {
    log(PREFIX + "&cModule unloaded.")
}

// --- COMMANDS ---

// Registers the command: /example-test
// 'sender' is the player or console executing the command.
// 'args' contains any arguments provided with the command.
command("example-test") { sender, args ->
    // Sends a confirmation message to the sender using the defined prefix.
    message(sender, PREFIX + "&eCongrats! Your new module is working.")

    // Opens a GUI (Inventory Menu) for the sender.
    // Title: "&8Test Menu", Size: 1 row (9 slots).
    gui(sender, "&8Test Menu", 1) {

        // Defines an item in slot 4 (Center of the 1st row).
        // Item: DIAMOND, Display Name: "&bPrize"
        slot(4, item(org.bukkit.Material.DIAMOND, "&bPrize")) { e ->
            // Click Event: Executed when the player clicks this item.
            message(e.whoClicked, "&aYou won a diamond!")
            e.whoClicked.closeInventory() // Closes the GUI
        }
    }
}

// --- EVENTS ---

// Listens for the PlayerJoinEvent (triggered when a player joins the server).
onEvent(org.bukkit.event.player.PlayerJoinEvent) { e ->
    // Broadcasts a welcome message to all players on the server.
    // Uses groovy string interpolation ${...} to insert the player's name.
    broadcast("&a${e.player.name} has joined. Welcome!")
}