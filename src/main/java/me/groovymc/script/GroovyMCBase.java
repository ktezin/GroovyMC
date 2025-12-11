package me.groovymc.script;

import groovy.lang.GroovyShell;
import groovy.sql.Sql;
import me.groovymc.GroovyMCPlugin;
import me.groovymc.controller.CommandRegistry;
import me.groovymc.db.FluentDatabase;
import me.groovymc.model.*;
import groovy.lang.Closure;
import groovy.lang.Script;
import me.groovymc.util.ChatUtils;
import me.groovymc.view.MessageView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
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

public abstract class GroovyMCBase extends Script {

    private JavaPlugin plugin;
    private ScriptModule module;
    private CommandRegistry commandRegistry;
    private final Map<String, SimpleSidebar> activeBoards = new HashMap<>();
    private Closure enableLogic;
    private Closure disableLogic;

    public void init(JavaPlugin plugin, ScriptModule module, CommandRegistry reg) {
        this.plugin = plugin;
        this.module = module;
        this.commandRegistry = reg;
    }

    public void include(String path) {
        File scriptFile = new File(module.getModuleFolder(), path);

        if (!scriptFile.exists()) {
            MessageView.logError("Script file not found: " + path);
            return;
        }

        try {
            CompilerConfiguration config = new CompilerConfiguration();
            config.setScriptBaseClass(GroovyMCBase.class.getName());

            GroovyShell shell = new GroovyShell(plugin.getClass().getClassLoader(), this.getBinding(), config);
            GroovyMCBase childScript = (GroovyMCBase) shell.parse(scriptFile);
            childScript.init(plugin, module, commandRegistry);
            childScript.run();

        } catch (Exception e) {
            MessageView.logError("An error occoured when loading " + path);
            e.printStackTrace();
        }
    }

    public void onEnable(Closure closure) {
        this.enableLogic = closure;
    }

    public void onDisable(Closure closure) {
        this.disableLogic = closure;

        activeBoards.values().forEach(SimpleSidebar::delete);
        activeBoards.clear();
    }

    public void doEnable() {
        if (enableLogic != null) enableLogic.call();
    }

    public void doDisable() {
        if (disableLogic != null) disableLogic.call();
    }


    public <T extends Event> void onEvent(Class<T> eventClass, Closure action) {
        Listener listener = new Listener() {
        };
        EventExecutor executor = (l, e) -> {
            if (eventClass.isInstance(e)) {
                action.call(e);
            }
        };

        Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.NORMAL, executor, plugin);
        module.addListener(listener);
    }

    public ScriptCommand command(String name, Closure action) {
        ScriptCommand cmd = new ScriptCommand(name, action);

        commandRegistry.register(cmd);
        module.addCommand(cmd);

        return cmd;
    }

    public void debug(Object msg) {
        if (module.isDebugMode()) {
            MessageView.log("&e[DEBUG] (" + module.getName() + "): &f" + msg.toString());
        }
    }

    public String color(Object text) {
        return ChatUtils.color(text);
    }

    public void message(Object target, Object msg) {
        if (target instanceof CommandSender) {
            ((CommandSender) target).sendMessage(ChatUtils.color(msg));
        }
    }

    public void broadcast(Object msg) {
        Bukkit.broadcastMessage(ChatUtils.color(msg));
    }

    public void log(Object msg) {
        Bukkit.getConsoleSender().sendMessage(ChatUtils.color(msg));
    }


    public void title(Object target, Object title, Object subtitle, int fadeIn, int stay, int fadeOut) {
        if (target instanceof Player) {
            ((Player) target).sendTitle(
                    ChatUtils.color(title),
                    ChatUtils.color(subtitle),
                    fadeIn, stay, fadeOut
            );
        }
    }

    public void title(Object target, Object title, Object subtitle) {
        title(target, title, subtitle, 10, 70, 20);
    }

    public void actionBar(Object target, Object msg) {
        if (target instanceof Player) {
            ((Player) target).sendActionBar(ChatUtils.color(msg));
        }
    }

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

    public BossBar bossBar(Object title, String colorStr) {
        return bossBar(title, colorStr, "SOLID", 1.0);
    }

    public int repeat(long delay, long period, Closure action) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    action.setDelegate(this);
                    action.setResolveStrategy(Closure.DELEGATE_FIRST);
                    action.call();
                } catch (Exception e) {
                    MessageView.logError("Repeat task error", e);
                    this.cancel();
                }
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, delay, period);
        int id = task.getTaskId();
        module.addTask(id);
        return id;
    }

    public int after(long delay, Closure action) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    action.setDelegate(this);
                    action.setResolveStrategy(Closure.DELEGATE_FIRST);
                    action.call();
                } catch (Exception e) {
                    MessageView.logError("After task error", e);
                }
            }
        };

        BukkitTask task = runnable.runTaskLater(plugin, delay);
        int id = task.getTaskId();
        module.addTask(id);
        return id;
    }

    public void finish(Object taskIdObj) {
        if (taskIdObj instanceof Number) {
            int id = ((Number) taskIdObj).intValue();
            Bukkit.getScheduler().cancelTask(id);
            module.removeTask(id);
        }
    }

    public void sidebar(Player player, Closure closure) {
        SimpleSidebar board = activeBoards.computeIfAbsent(player.getName(), k -> new SimpleSidebar(player, "Stats"));

        SimpleSidebar.SidebarConfig config = new SimpleSidebar.SidebarConfig();

        closure.setDelegate(config);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();

        if (config.title != null) board.setTitle(config.title);
        if (config.lines != null) board.setLines(config.lines);
    }

    public void removeSidebar(Player player) {
        if (activeBoards.containsKey(player.getName())) {
            activeBoards.get(player.getName()).delete();
            activeBoards.remove(player.getName());
        }
    }

    public void async(Closure action) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> action.call());
    }

    public void sync(Closure action) {
        if (Bukkit.isPrimaryThread()) {
            action.call();
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> action.call());
        }
    }

    public FluentDatabase getDb() {
        return new FluentDatabase(new Sql(GroovyMCPlugin.dbManager.getDataSource()), module.getName());
    }

    public void gui(Player player, String title, int rows, Closure setup) {
        GuiHolder holder = new GuiHolder();

        Inventory inv = Bukkit.createInventory(holder, rows * 9, ChatUtils.color(title));
        holder.setInventory(inv);

        GuiBuilder builder = new GuiBuilder(inv, holder);

        setup.setDelegate(builder);
        setup.setResolveStrategy(Closure.DELEGATE_FIRST);
        setup.call();

        player.openInventory(inv);
    }

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

    public ItemStack item(Material mat, String name) {
        return item(mat, name, null);
    }

    public ItemStack item(Material mat) {
        return item(mat, null, null);
    }

    public class GuiBuilder {
        private final Inventory inv;
        private final GuiHolder holder;

        public GuiBuilder(Inventory inv, GuiHolder holder) {
            this.inv = inv;
            this.holder = holder;
        }

        public void slot(int slot, ItemStack item, Closure action) {
            inv.setItem(slot, item);
            if (action != null) {
                holder.setAction(slot, action);
            }
        }

        public void background(ItemStack item) {
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    inv.setItem(i, item);
                }
            }
        }
    }

    public ModuleConfig getConfig() {
        return module.getConfig();
    }

    public void saveConfig() {
        module.getConfig().save();
    }

    public void reloadConfig() {
        module.getConfig().reload();
    }

    public NbtWrapper nbt(Object target) {
        return new NbtWrapper(plugin, target);
    }
}