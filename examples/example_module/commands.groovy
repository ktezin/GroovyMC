package example_module

import me.groovymc.api.ScriptAPI
import groovy.transform.BaseScript
import org.bukkit.entity.Player

@BaseScript ScriptAPI base

// --- COMMAND REGISTRATION ---
// Registers the main command '/example'.
// The logic block handles the execution of the command.
command("example") { sender, args ->
    // 1. Validation: Check if the sender is a Player (block console usage).
    if (!(sender instanceof Player)) {
        message(sender, "Only players can execute this command")
        return
    }

    def p = (Player) sender

    // 2. Check Arguments: If no arguments are provided, show usage help.
    if (args.length == 0) {
        message(p, "&r* /example <first/second>")
        return
    }

    // 3. Sub-command Logic: Switch based on the first argument (args[0]).
    switch (args[0]) {
        case "first":
            message(p, "&rFirst")
            break
        case "second":
            message(p, "&rSecond")
            break
    }
// --- TAB COMPLETION ---
// Chains the .tabComplete closure to the command definition.
// This handles suggestions when the player presses the TAB key.
}.tabComplete { sender, args ->

    // Suggest options for the FIRST argument (args[0])
    if (args.length == 1) {
        def suggestions = ["first", "second"]

        // Return suggestions filtered by what the user has already typed.
        // E.g., if they typed "fi", it only suggests "first".
        return suggestions.findAll { it.startsWith(args[0].toLowerCase()) }
    }

    // Logic for the SECOND argument
    if (args.length == 2) {
        return null // Returning 'null' typically defaults to showing online player names in Bukkit.
    }

    // Return an empty list to show NO suggestions for other arguments.
    return []
}