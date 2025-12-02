package me.groovymc.script;

import groovy.lang.GroovyShell;
import me.groovymc.controller.CommandRegistry;
import me.groovymc.model.ScriptModule;
import groovy.lang.Closure;
import groovy.lang.Script;
import me.groovymc.model.SimpleSidebar;
import me.groovymc.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.util.HashMap;
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
        // 1. Dosyayı bul (Alt klasör destekler)
        File scriptFile = new File(module.getModuleFolder(), path);

        if (!scriptFile.exists()) {
            log("&c[Hata] Dosya bulunamadı: " + path);
            return;
        }

        try {
            // 2. Ayarları Kopyala (Aynı Base Class)
            CompilerConfiguration config = new CompilerConfiguration();
            config.setScriptBaseClass(GroovyMCBase.class.getName());

            // 3. AYNI BINDING'i Kullan (Sihir burada!)
            // 'getBinding()' mevcut scriptin hafızasıdır. Bunu yeni scripte verirsek
            // değişkenleri paylaşırlar.
            GroovyShell shell = new GroovyShell(plugin.getClass().getClassLoader(), this.getBinding(), config);

            // 4. Parse et
            GroovyMCBase childScript = (GroovyMCBase) shell.parse(scriptFile);

            // 5. Bağımlılıkları aktar (Komut, Event sistemleri çalışsın diye)
            childScript.init(plugin, module, commandRegistry);

            // 6. Çalıştır (Dosyadaki kodlar işlensin)
            childScript.run();

        } catch (Exception e) {
            log("&c[Hata] " + path + " yüklenirken sorun oluştu!");
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
        Listener listener = new Listener() {};
        EventExecutor executor = (l, e) -> {
            // "isInstance" kontrolü ile güvenli cast
            if (eventClass.isInstance(e)) {
                action.call(e); // Event objesini Closure'a parametre olarak gönder
            }
        };

        Bukkit.getPluginManager().registerEvent(eventClass, listener, EventPriority.NORMAL, executor, plugin);
        module.addListener(listener);
    }

    public void command(String name, Closure action) {
        Command cmd = new Command(name) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                action.call(sender, args);
                return true;
            }
        };

        commandRegistry.register(cmd);
        module.addCommand(cmd);
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

    public void repeat(long delay, long period, Closure action) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> action.call(), delay, period);
        module.addTask(task.getTaskId());
    }

    public void after(long delay, Closure action) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> action.call(), delay);
        module.addTask(task.getTaskId());
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
}