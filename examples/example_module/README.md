# Example Module for GroovyMC

This is a comprehensive example module demonstrating the capabilities of the **GroovyMC** scripting API. It covers essential features such as database management, configuration handling, custom commands, GUIs, sidebars, and bossbars.

## File Structure

- **main.groovy**: The entry point of the module. Handles imports and lifecycle logic (`onEnable`/`onDisable`).
- **init_config.groovy**: Shows how to manage `config.yml`, save/load custom objects (Locations), and use schedulers.
- **init_db.groovy**: Demonstrates creating tables, inserting data, and running async database queries.
- **commands.groovy**: Example of command registration with arguments and Tab Completion.
- **events.groovy**: Handling Bukkit events with visual effects (Particles, Sounds).
- **sidebar.groovy**: Creating and updating per-player scoreboards efficiently.
- **bossbar.groovy**: Managing BossBars with animations and countdowns.

## How to Install

1. Ensure **GroovyMC** is installed on your server.
2. Download this `example_module` folder.
3. Place the entire folder into `plugins/GroovyMC/modules/`.
4. Run `/groovy reload` or restart your server.
5. Enjoy exploring the code!

## Commands to Try

- `/example-test`: Opens a test GUI.
- `/example <first/second>`: Tests command arguments and tab completion.
- `/stats`: Displays database-stored statistics.
- `/setspawn`: Saves your location to the config.
- `/randomtheme`: Picks a random string from the config.
- `/custombar`: Starts a BossBar countdown animation.