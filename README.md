
# GroovyMC
![Status](https://img.shields.io/badge/Status-Work_in_Progress-orange)
![Version](https://img.shields.io/badge/Version-v0.1.0_Alpha-blue)
> ⚠️ **Note:** This project is currently under **active development**. APIs and features are subject to change. Not recommended for production servers yet.

A lightweight, modular Groovy script engine for Minecraft (Paper/Spigot) servers. It allows you to develop game logic, commands, and events dynamically without restarting the server.

## Features

- **Hot Reloading:** Changes in scripts are detected and applied automatically.
- **Modular Structure:** Organize scripts into separate folders and sub-modules.
- **Simple DSL:** Clean syntax for Commands, Events, Schedulers, and Scoreboards.
- **Includes:** Split your code into multiple files using `include 'path/to/file.groovy'`.
- **Global Binding:** Share variables and functions across included files.

## Installation

1. Build the project using Maven:
   ```sh
   mvn clean package

2.  Place the `GroovyMC.jar` file into your server's `plugins` folder.
3.  Restart the server.

## Getting Started

Scripts are located in `plugins/GroovyMC/modules/`.

### Folder Structure

```text
plugins/GroovyMC/modules/
└── my_module/
    ├── main.groovy       <-- Entry point
    ├── commands.groovy   <-- Optional: separated commands
    └── utils.groovy      <-- Optional: shared functions
```

### Example Script (`main.groovy`)

```groovy
import org.bukkit.event.player.PlayerJoinEvent

// Global variables (accessible from included files)
PREFIX = color("&8[&6GroovyMC&8] ")

// Import other files
include 'commands.groovy'

onEnable {
    log("&aModule enabled successfully!")
}

onDisable {
    log("&cModule disabled.")
}

// Event Listener
onEvent(PlayerJoinEvent) { e ->
    broadcast("&e${e.player.name} &7joined the server!")
    
    // Delayed task (2 seconds)
    after(40) {
        message(e.player, "${PREFIX} &fWelcome to the server!")
    }
}

// Repeating Task (Every 1 second)
repeat(0, 20) {
    // Logic here...
}
```

### Commands API (`commands.groovy`)

```groovy
import org.bukkit.entity.Player

command("heal") {sender, args ->
    if (!(sender instanceof Player)) {
        message(sender, "&cOnly players can use this command!")
        return
    }
    Player p = sender as Player
    p.setHealth(20d)
    message(p, "&aYou are healed!")
}
```

### Scoreboard API

Easily create flicker-free sidebars.

```groovy
onEvent(org.bukkit.event.player.PlayerJoinEvent) { e ->
    updateBoard(e.player)
}

def updateBoard(p) {
    sidebar(p) {
        title = "&6&lStats"
        lines = [
            "&7---------------",
            "&fName: &e${p.name}",
            "&fPing: &a${p.ping} ms",
            "&7---------------"
        ]
    }
}
```

## IDE Setup (Intellisense)

To get code completion in IntelliJ IDEA:

1. Create a new Java project in GroovyMC folder for your scripts.
2. Add `GroovyMC.jar`, `Paper-API.jar` and `Groovy.jar` as libraries.
3. Add the following header to your script files:

```groovy
import me.groovymc.script.GroovyMCBase
import groovy.transform.BaseScript
@BaseScript GroovyMCBase base
```