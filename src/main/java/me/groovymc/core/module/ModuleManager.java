package me.groovymc.core.module;

import com.mojang.brigadier.Message;
import groovy.lang.GroovyCodeSource;
import me.groovymc.api.ScriptAPI;
import me.groovymc.features.command.CommandRegistry;
import me.groovymc.view.MessageView;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class ModuleManager {
    private final JavaPlugin plugin;
    private final CommandRegistry commandRegistry;

    private final Map<String, ScriptModule> modules = new HashMap<>();
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final File modulesFolder;

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;

        this.commandRegistry = new CommandRegistry(plugin);
        this.modulesFolder = new File(plugin.getDataFolder(), "modules");

        if (!modulesFolder.exists()) modulesFolder.mkdirs();

        Bukkit.getScheduler().runTaskTimer(plugin, this::watchFiles, 40L, 40L);
    }

    public boolean createModule(String name) {
        name = name.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();

        File folder = new File(modulesFolder, name);
        if (folder.exists()) {
            return false;
        }

        folder.mkdirs();
        File mainFile = new File(folder, "main.groovy");

        String template = """
        // GroovyMC Module: %s
        // Created At: %s
        package %s
        
        import me.groovymc.api.ScriptAPI
        import groovy.transform.BaseScript
        @BaseScript ScriptAPI base
        
        // Global variables
        PREFIX = color("&8[&b%s&8] ")
        
        onEnable {
            log(PREFIX + "&aModule successfully loaded!")
        }
        
        onDisable {
            log(PREFIX + "&cModule unloaded.")
        }
        
        // Example Command: /%s-test
        command("%s-test") { sender, args ->
            message(sender, PREFIX + "&eCongrats! Your new module is working.")
            
            // Example GUI
            gui(sender, "&8Test Menu", 1) {
                slot(4, item(org.bukkit.Material.DIAMOND, "&bPrize")) { e ->
                    message(e.whoClicked, "&aYou won a diamond!")
                    e.whoClicked.closeInventory()
                }
            }
        }
        
        // Example Event
        onEvent(org.bukkit.event.player.PlayerJoinEvent) { e ->
            broadcast("&a${e.player.name} has joined. Welcome!")
        }
        """.formatted(name, new java.util.Date().toString(), name, name, name, name);

        try {
            Files.writeString(mainFile.toPath(), template, StandardOpenOption.CREATE);

            loadModule(name);
            return true;
        } catch (Exception e) {
            MessageView.logError("Error while creating module: " + name, e);
            return false;
        }
    }

    private void watchFiles() {
        if (!modulesFolder.exists()) return;

        File[] files = modulesFolder.listFiles();
        if (files == null) return;

        for (File folder : files) {
            if (folder.isDirectory()) {
                String name = folder.getName();

                if (name.startsWith("_")) continue;

                File main = new File(folder, "main.groovy");

                if (!main.exists()) continue;

                long currentModified = calculateFolderLastModified(folder);
                Long lastKnown = fileTimestamps.get(name);

                if (lastKnown == null || currentModified > lastKnown) {
                    if (lastKnown != null) {
                        MessageView.log("Changes detected: " + name + ", module reloading...");
                    }

                    loadModule(name);

                    fileTimestamps.put(name, currentModified);
                }
            }
        }

        new HashMap<>(modules).keySet().forEach(activeName -> {
            File folder = new File(modulesFolder, activeName);
            if (!folder.exists()) {
                unloadModule(activeName);
                MessageView.log("Module not found, deactivated: " + activeName);
            }
        });
    }

    private long calculateFolderLastModified(File file) {
        if (file.isFile()) {
            if (file.getName().endsWith(".groovy")) {
                return file.lastModified();
            }
            return 0;
        }

        long maxTime = 0;
        File[] children = file.listFiles();

        if (children != null) {
            for (File child : children) {
                // Recursive
                long time = calculateFolderLastModified(child);
                if (time > maxTime) {
                    maxTime = time;
                }
            }
        }
        return maxTime;
    }

    public void loadModule(String name) {
        if (name.startsWith("_")) return;

        unloadModule(name);

        File mainFile = new File(modulesFolder, name + "/main.groovy");
        if (!mainFile.exists()) return;

        ScriptModule module = new ScriptModule(name, mainFile);

        try {
            CompilerConfiguration config = new CompilerConfiguration();
            config.setScriptBaseClass(ScriptAPI.class.getName());

            GroovyShell shell = new GroovyShell(plugin.getClass().getClassLoader(), new Binding(), config);

            String scriptContent = Files.readString(mainFile.toPath(), StandardCharsets.UTF_8);
            String virtualName = name + "_main.groovy";

            GroovyCodeSource codeSource = new GroovyCodeSource(scriptContent, virtualName, "/groovy/script");
            codeSource.setCachable(false);

            ScriptModule finalModule = module;
            ScriptAPI script = (ScriptAPI) shell.parse(codeSource);

            script.init(plugin, finalModule, commandRegistry);
            script.run();
            script.doEnable();

            module.setScriptInstance(script);
            modules.put(name, module);
            MessageView.log("Module loaded: " + name);
        } catch (Exception e) {
            MessageView.logScriptError(name, e);
            module.cleanup();
        }
    }

    public void unloadModule(String name) {
        ScriptModule module = modules.remove(name);
        if (module != null) {
            try {
                if (module.getScriptInstance() instanceof ScriptAPI) {
                    ((ScriptAPI) module.getScriptInstance()).doDisable();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            module.getCommands().forEach(commandRegistry::unregister);
            module.cleanup();

            fileTimestamps.remove(name);
        }
    }

    public void reloadModule(String name) {
        unloadModule(name);
        loadModule(name);
    }

    public void loadAll() {
        if (modulesFolder.listFiles() == null) return;
        long started = System.currentTimeMillis();
        for (File f : modulesFolder.listFiles()) {
            if (f.isDirectory() && !f.getName().startsWith("_")) {
                loadModule(f.getName());
                fileTimestamps.put(f.getName(), calculateFolderLastModified(f));
            }
        }
        if (modules.isEmpty()) {
            MessageView.log("No active modules found. You can create your first module using /groovymc create <name>");
            return;
        }
        long now = ((System.currentTimeMillis() - started) / 1000) % 60;
        MessageView.log("Total of " + modules.size() + " modules loaded in " + now + "s");
    }

    public void unloadAll() {
        new HashMap<>(modules).keySet().forEach(this::unloadModule);
    }

    public boolean toggleModuleState(String name, boolean enable) {
        File folder = new File(modulesFolder, name);
        File disabledFolder = new File(modulesFolder, "_" + name);

        if (enable) {
            if (disabledFolder.exists()) {
                return disabledFolder.renameTo(folder);
            }
        } else {
            if (folder.exists()) {
                unloadModule(name);
                return folder.renameTo(disabledFolder);
            }
        }
        return false;
    }

    public void toggleDebug(String name) {
        if (modules.containsKey(name)) {
            ScriptModule m = modules.get(name);
            m.setDebugMode(!m.isDebugMode());
            MessageView.log(name + " debug mode: " + (m.isDebugMode() ? "&aON" : "&cOFF"));
        } else {
            MessageView.log("&cThe module was either not found or not loaded.");
        }
    }

    public Map<String, ScriptModule> getModules() {
        return modules;
    }
}