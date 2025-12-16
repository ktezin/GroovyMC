package me.groovymc.api;

import groovy.lang.DelegatesTo;
import groovy.lang.GroovyShell;
import groovy.sql.Sql;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;
import groovy.transform.stc.SimpleType;
import me.groovymc.GroovyMC;
import me.groovymc.core.module.ModuleConfig;
import me.groovymc.core.module.ScriptModule;
import me.groovymc.features.command.CommandRegistry;
import me.groovymc.features.db.FluentDatabase;
import me.groovymc.features.command.ScriptCommand;
import me.groovymc.features.gui.GuiHolder;
import me.groovymc.features.nbt.NbtWrapper;
import me.groovymc.features.sidebar.SidebarBuilder;
import groovy.lang.Closure;
import groovy.lang.Script;
import me.groovymc.util.ChatUtils;
import me.groovymc.view.MessageView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The core API class for GroovyMC scripts.
 * <p>
 * All scripts in the module system extend this class, providing direct access to
 * helper methods for Bukkit events, commands, schedulers, database operations, and GUIs.
 */
public abstract class ScriptAPI extends Script {

    private JavaPlugin plugin;
    private ScriptModule module;
    private CommandRegistry commandRegistry;
    private final Map<String, SidebarBuilder> activeBoards = new HashMap<>();
    private Closure enableLogic;
    private Closure disableLogic;

    /**
     * Internal initialization method. Do not call this manually.
     */
    public void init(JavaPlugin plugin, ScriptModule module, CommandRegistry reg) {
        this.plugin = plugin;
        this.module = module;
        this.commandRegistry = reg;
    }

    /**
     * Includes and executes another Groovy script file from the module folder.
     * Useful for splitting large scripts into smaller, modular files.
     *
     * @param path The relative path to the script file (e.g., "utils/math.groovy").
     */
    public void include(String path) {
        File scriptFile = new File(module.getModuleFolder(), path);

        if (!scriptFile.exists()) {
            MessageView.logError("Script file not found: " + path);
            return;
        }

        try {
            CompilerConfiguration config = new CompilerConfiguration();
            config.setScriptBaseClass(ScriptAPI.class.getName());

            GroovyShell shell = new GroovyShell(plugin.getClass().getClassLoader(), this.getBinding(), config);
            ScriptAPI childScript = (ScriptAPI) shell.parse(scriptFile);
            childScript.init(plugin, module, commandRegistry);
            childScript.run();

        } catch (Exception e) {
            MessageView.logError("An error occurred when loading " + path);
            e.printStackTrace();
        }
    }

    /**
     * Defines logic to run when the module is enabled.
     *
     * @param closure The code block to execute on enable.
     */
    public void onEnable(Closure closure) {
        this.enableLogic = closure;
    }

    /**
     * Defines logic to run when the module is disabled (server stop or reload).
     *
     * @param closure The code block to execute on disable.
     */
    public void onDisable(Closure closure) {
        this.disableLogic = closure;

        activeBoards.values().forEach(SidebarBuilder::delete);
        activeBoards.clear();
    }

    /**
     * Internal execution of the enable logic.
     */
    public void doEnable() {
        if (enableLogic == null) return;

        try {
            enableLogic.call();
        } catch (Exception e) {
            MessageView.logScriptError(module.getName(), e);
        }
    }

    /**
     * Internal execution of the disable logic.
     */
    public void doDisable() {
        if (disableLogic == null) return;

        try {
            disableLogic.call();
        } catch (Exception e) {
            MessageView.logScriptError(module.getName(), e);
        }
    }

    /**
     * Registers a listener for a specific Bukkit Event.
     *
     * @param eventClass The class of the event to listen for (e.g., PlayerJoinEvent.class).
     * @param action     The closure to execute when the event fires. The event object is passed as an argument.
     * @param <T>        The event type.
     */
    public <T extends Event> void onEvent(Class<T> eventClass, @ClosureParams(FirstParam.FirstGenericType.class) Closure action) {
        Listener listener = new Listener() {
        };

        EventExecutor executor = (l, e) -> {
            if (eventClass.isInstance(e)) {
                try {
                    action.call(e);
                } catch (Exception ex) {
                    MessageView.logScriptError(module.getName(), ex);
                }
            }
        };

        Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.NORMAL, executor, plugin);
        module.addListener(listener);
    }

    /**
     * Registers a new command for the server.
     *
     * @param name   The name of the command (without the slash).
     * @param action The closure to execute when the command is run. parameters: (sender, args).
     * @return The created ScriptCommand object, allowing for method chaining (e.g., .tabComplete).
     */
    public ScriptCommand command(String name, @ClosureParams(value = SimpleType.class, options = {"org.bukkit.command.CommandSender", "java.lang.String[]"}) Closure action) {
        ScriptCommand cmd = new ScriptCommand(name, module.getName(), action);

        commandRegistry.register(cmd);
        module.addCommand(cmd);

        return cmd;
    }

    /**
     * Logs a debug message to the console if debug mode is enabled for this module.
     *
     * @param msg The object/message to log.
     */
    public void debug(Object msg) {
        if (module.isDebugMode()) {
            MessageView.log("&e[DEBUG] (" + module.getName() + "): &f" + msg.toString());
        }
    }

    /**
     * Translates '&' color codes in a string.
     *
     * @param text The text to colorize.
     * @return The colored string.
     */
    public String color(Object text) {
        return ChatUtils.color(text);
    }

    /**
     * Sends a colored message to a target (Player or Console).
     *
     * @param target The CommandSender to receive the message.
     * @param msg    The message content (supports '&' color codes).
     */
    public void message(Object target, Object msg) {
        if (target instanceof CommandSender) {
            ((CommandSender) target).sendMessage(ChatUtils.color(msg));
        }
    }

    /**
     * Broadcasts a message to all players on the server.
     *
     * @param msg The message content.
     */
    public void broadcast(Object msg) {
        Bukkit.broadcastMessage(ChatUtils.color(msg));
    }

    /**
     * Logs a message to the server console.
     *
     * @param msg The message content.
     */
    public void log(Object msg) {
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color(msg));
    }

    /**
     * Sends a title and subtitle to a player.
     *
     * @param target   The player to receive the title.
     * @param title    The main title text.
     * @param subtitle The subtitle text.
     * @param fadeIn   Fade-in time in ticks.
     * @param stay     Stay time in ticks.
     * @param fadeOut  Fade-out time in ticks.
     */
    public void title(Object target, Object title, Object subtitle, int fadeIn, int stay, int fadeOut) {
        if (target instanceof Player) {
            ((Player) target).sendTitle(
                    ChatUtils.color(title),
                    ChatUtils.color(subtitle),
                    fadeIn, stay, fadeOut
            );
        }
    }

    /**
     * Sends a title with default timing (10in, 70stay, 20out) to a player.
     *
     * @param target   The player.
     * @param title    The main title.
     * @param subtitle The subtitle.
     */
    public void title(Object target, Object title, Object subtitle) {
        title(target, title, subtitle, 10, 70, 20);
    }

    /**
     * Sends an action bar message (text above the hotbar) to a player.
     *
     * @param target The player.
     * @param msg    The message content.
     */
    public void actionBar(Object target, Object msg) {
        if (target instanceof Player) {
            ((Player) target).sendActionBar(ChatUtils.color(msg));
        }
    }

    /**
     * Creates and registers a BossBar.
     *
     * @param title    The title of the bar.
     * @param colorStr The color (e.g., "RED", "BLUE").
     * @param styleStr The style (e.g., "SOLID", "SEGMENTED_6").
     * @param progress The progress (0.0 to 1.0).
     * @return The created BossBar instance.
     */
    public BossBar bossBar(Object title, String colorStr, String styleStr, double progress) {
        BarColor color;
        try {
            color = BarColor.valueOf(colorStr.toUpperCase());
        } catch (Exception e) {
            color = BarColor.WHITE;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(styleStr.toUpperCase());
        } catch (Exception e) {
            style = BarStyle.SOLID;
        }

        BossBar bar = Bukkit.createBossBar(ChatUtils.color(title), color, style);
        bar.setProgress(progress);

        module.addBossBar(bar);

        return bar;
    }

    /**
     * Creates a solid style BossBar with full progress.
     *
     * @param title    The title of the bar.
     * @param colorStr The color.
     * @return The created BossBar instance.
     */
    public BossBar bossBar(Object title, String colorStr) {
        return bossBar(title, colorStr, "SOLID", 1.0);
    }

    /**
     * Starts a repeating task.
     *
     * @param delay  Delay before first execution (in ticks).
     * @param period Time between executions (in ticks).
     * @param action The closure to execute. Calling {@code cancel()} inside stops the task.
     * @return The task ID.
     */
    public int repeat(long delay, long period, @DelegatesTo(value = BukkitRunnable.class, strategy = Closure.DELEGATE_FIRST) Closure action) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    action.setDelegate(this);
                    action.setResolveStrategy(Closure.DELEGATE_FIRST);
                    action.call();
                } catch (Exception e) {
                    MessageView.logScriptError(module.getName(), e);
                    this.cancel();
                }
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, delay, period);
        int id = task.getTaskId();
        module.addTask(id);
        return id;
    }

    /**
     * execute a task after a specified delay.
     *
     * @param delay  The delay in ticks.
     * @param action The closure to execute.
     * @return The task ID.
     */
    public int after(long delay, @DelegatesTo(value = BukkitRunnable.class, strategy = Closure.DELEGATE_FIRST) Closure action) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    action.setDelegate(this);
                    action.setResolveStrategy(Closure.DELEGATE_FIRST);
                    action.call();
                } catch (Exception e) {
                    MessageView.logScriptError(module.getName(), e);
                }
            }
        };

        BukkitTask task = runnable.runTaskLater(plugin, delay);
        int id = task.getTaskId();
        module.addTask(id);
        return id;
    }

    /**
     * Manually cancels a running task by its ID.
     *
     * @param taskIdObj The task ID (integer).
     */
    public void finish(Object taskIdObj) {
        if (taskIdObj instanceof Number) {
            int id = ((Number) taskIdObj).intValue();
            Bukkit.getScheduler().cancelTask(id);
            module.removeTask(id);
        }
    }

    /**
     * Creates or updates a sidebar (scoreboard) for a player.
     *
     * @param player  The player to display the sidebar to.
     * @param closure The configuration closure (set 'title' and 'lines').
     */
    public void sidebar(Player player, @DelegatesTo(value = SidebarBuilder.SidebarConfig.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        SidebarBuilder board = activeBoards.computeIfAbsent(player.getName(), k -> new SidebarBuilder(player, "Stats"));

        SidebarBuilder.SidebarConfig config = new SidebarBuilder.SidebarConfig();

        closure.setDelegate(config);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        try {
            closure.call();
        } catch (Exception e) {
            MessageView.logScriptError(module.getName(), e);
            return;
        }

        if (config.title != null) board.setTitle(config.title);
        if (config.lines != null) board.setLines(config.lines);
    }

    /**
     * Removes the custom sidebar from a player.
     *
     * @param player The player.
     */
    public void removeSidebar(Player player) {
        if (activeBoards.containsKey(player.getName())) {
            activeBoards.get(player.getName()).delete();
            activeBoards.remove(player.getName());
        }
    }

    /**
     * Runs a task asynchronously (off the main thread).
     * WARNING: Do not call most Bukkit API methods here.
     *
     * @param action The closure to execute.
     */
    public void async(Closure action) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                action.call();
            } catch (Exception e) {
                MessageView.logScriptError(module.getName(), e);
            }
        });
    }

    /**
     * Runs a task synchronously (on the main server thread).
     * Safe for all Bukkit API calls.
     *
     * @param action The closure to execute.
     */
    public void sync(Closure action) {
        if (Bukkit.isPrimaryThread()) {
            try {
                action.call();
            } catch (Exception e) {
                MessageView.logScriptError(module.getName(), e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    action.call();
                } catch (Exception e) {
                    MessageView.logScriptError(module.getName(), e);
                }
            });
        }
    }

    /**
     * Gets the database instance for this module.
     * Accessible in scripts as 'db'.
     *
     * @return The FluentDatabase instance.
     */
    public FluentDatabase getDb() {
        return new FluentDatabase(new Sql(GroovyMC.dbManager.getDataSource()), module.getName());
    }

    /**
     * Opens a GUI (Inventory Menu) for a player.
     *
     * @param player The player to open the GUI for.
     * @param title  The title of the inventory.
     * @param rows   The number of rows (1-6).
     * @param setup  The closure to configure items and clicks.
     */
    public void gui(Player player, String title, int rows, @DelegatesTo(value = GuiBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure setup) {
        GuiHolder holder = new GuiHolder();

        Inventory inv = Bukkit.createInventory(holder, rows * 9, ChatUtils.color(title));
        holder.setInventory(inv);

        GuiBuilder builder = new GuiBuilder(inv, holder);

        setup.setDelegate(builder);
        setup.setResolveStrategy(Closure.DELEGATE_FIRST);
        try {
            setup.call();
        } catch (Exception e) {
            MessageView.logScriptError(module.getName(), e);
            return;
        }

        player.openInventory(inv);
    }

    /**
     * Creates an ItemStack with metadata.
     *
     * @param mat  The material type.
     * @param name The display name.
     * @param lore The lore lines.
     * @return The configured ItemStack.
     */
    public ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        if (mat == null || mat.isAir()) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(ChatUtils.color(name));
            if (lore != null) meta.setLore(ChatUtils.color(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates an ItemStack with a name.
     *
     * @param mat  The material type.
     * @param name The display name.
     * @return The configured ItemStack.
     */
    public ItemStack item(Material mat, String name) {
        return item(mat, name, null);
    }

    /**
     * Creates a simple ItemStack.
     *
     * @param mat The material type.
     * @return The ItemStack.
     */
    public ItemStack item(Material mat) {
        return item(mat, null, null);
    }

    /**
     * Helper class for building GUIs.
     * Methods inside 'gui' closure delegate to this class.
     */
    public class GuiBuilder {
        private final Inventory inv;
        private final GuiHolder holder;

        public GuiBuilder(Inventory inv, GuiHolder holder) {
            this.inv = inv;
            this.holder = holder;
        }

        /**
         * Sets an item in a specific slot and assigns a click action.
         *
         * @param slot   The slot index (0-based).
         * @param item   The item to place.
         * @param action The closure to run when clicked.
         */
        public void slot(int slot, ItemStack item, @ClosureParams(value = SimpleType.class, options = "org.bukkit.event.inventory.InventoryClickEvent") Closure action) {
            inv.setItem(slot, item);
            if (action != null) {
                holder.setAction(slot, action);
            }
        }

        /**
         * Fills all empty slots with a specific item.
         *
         * @param item The background item.
         */
        public void background(ItemStack item) {
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    inv.setItem(i, item);
                }
            }
        }
    }

    /**
     * Gets the configuration object for this module.
     * Accessible in scripts as 'config'.
     *
     * @return The ModuleConfig instance.
     */
    public ModuleConfig getConfig() {
        return module.getConfig();
    }

    /**
     * Saves any changes made to the configuration to disk.
     */
    public void saveConfig() {
        module.getConfig().save();
    }

    /**
     * Reloads the configuration from disk, discarding unsaved changes.
     */
    public void reloadConfig() {
        module.getConfig().reload();
    }

    /**
     * Wraps an object (Item or Entity) to manipulate its NBT data.
     *
     * @param target The object to wrap.
     * @return A new NbtWrapper instance.
     */
    public NbtWrapper nbt(Object target) {
        return new NbtWrapper(plugin, target);
    }
}