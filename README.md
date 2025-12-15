# GroovyMC
![Status](https://img.shields.io/badge/Status-Work_in_Progress-orange)
![Version](https://img.shields.io/github/v/release/ktezin/GroovyMC?include_prereleases&color=blue&style=for-the-badge)
> ⚠️ **Note:** This project is currently under **active development**. APIs and features are subject to change. Not recommended for production servers yet.


GroovyMC allows you to write Minecraft plugins using Groovy scripts. It is designed to be simple, fast, and supports hot-reloading. You can make changes to your code without restarting the server.

Here is how simple it is to create a command and register an event:

```groovy
// Register a command
command("hello") { sender, args ->
    message(sender, "&aHello from GroovyMC!")
}

// Listen for an event
onEvent(org.bukkit.event.player.PlayerJoinEvent) { e ->
    broadcast("&e" + e.player.name + " joined the server.")
}
```

## Quick Start

You can create your first module directly from the game or from the server console.

1. Run the command:
   `/groovymc create my_module`

2. This will create a new folder at `plugins/GroovyMC/modules/my_module/` with a ready-to-use script.

3. Open the file `main.groovy` inside that folder and edit it.

4.  Save your file and your changes are now live.

## Installing Modules

To install a module created by someone else:

1.  Download the module folder.
2.  Place the folder into `plugins/GroovyMC/modules/`.
3.  Run the command `/groovymc reload`.

## Examples and Documentation

For more advanced features like Database connectivity, GUIs, Scoreboards, and Config management, please check the **examples** folder.

* **[Browse Examples](./examples/README.md)**
* **[Comprehensive Example Module](./examples/example_module/README.md)** (Recommended for learning)

## Developer Note

If you are using IntelliJ Idea open `plugins/GroovyMC/` as a project for full code completion, javadocs and Intellisense.