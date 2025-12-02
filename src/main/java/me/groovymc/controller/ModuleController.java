package me.groovymc.controller;

import me.groovymc.model.ScriptModule;
import me.groovymc.script.GroovyMCBase;
import me.groovymc.view.MessageView;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModuleController {
    private final JavaPlugin plugin;
    private final MessageView view;
    private final CommandRegistry commandRegistry;

    private final Map<String, ScriptModule> modules = new HashMap<>();
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final File modulesFolder;

    public ModuleController(JavaPlugin plugin, MessageView view) {
        this.plugin = plugin;
        this.view = view;

        this.commandRegistry = new CommandRegistry(plugin);
        this.modulesFolder = new File(plugin.getDataFolder(), "modules");

        if (!modulesFolder.exists()) modulesFolder.mkdirs();

        Bukkit.getScheduler().runTaskTimer(plugin, this::watchFiles, 40L, 40L);
    }

    private void watchFiles() {
        if (!modulesFolder.exists()) return;

        File[] files = modulesFolder.listFiles();
        if (files == null) return;

        for (File folder : files) {
            if (folder.isDirectory()) {
                File main = new File(folder, "main.groovy");

                if (main.exists()) {
                    String name = folder.getName();

                    long currentModified = calculateFolderLastModified(folder);

                    if (!modules.containsKey(name)) {
                        loadModule(name);
                        fileTimestamps.put(name, currentModified);
                    }
                    else {
                        Long lastKnown = fileTimestamps.get(name);
                        if (lastKnown == null || currentModified > lastKnown) {
                            view.log("Changes detected: " + name + ", module reloading...");
                            reloadModule(name);
                            fileTimestamps.put(name, currentModified);
                        }
                    }
                }
            }
        }
    }

    private long calculateFolderLastModified(File file) {
        if (file.isFile()) {
            return file.lastModified();
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
        unloadModule(name);

        File mainFile = new File(modulesFolder, name + "/main.groovy");
        if (!mainFile.exists()) return;

        ScriptModule module = new ScriptModule(name, mainFile);

        try {
            CompilerConfiguration config = new CompilerConfiguration();
            config.setScriptBaseClass(GroovyMCBase.class.getName());

            GroovyShell shell = new GroovyShell(plugin.getClass().getClassLoader(), new Binding(), config);

            ScriptModule finalModule = module;
            GroovyMCBase script = (GroovyMCBase) shell.parse(mainFile);

            script.init(plugin, finalModule, commandRegistry);

            script.run();

            script.doEnable();

            module.setScriptInstance(script);
            modules.put(name, module);
            view.log("Module loaded: " + name);

        } catch (Exception e) {
            view.logError("An error occured while loading module: " + name, e);
        }
    }

    public void unloadModule(String name) {
        ScriptModule module = modules.remove(name);
        if (module != null) {
            try {
                if (module.getScriptInstance() instanceof GroovyMCBase) {
                    ((GroovyMCBase) module.getScriptInstance()).doDisable();
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
        for (File f : modulesFolder.listFiles()) {
            if (f.isDirectory()) loadModule(f.getName());
        }
    }

    public void unloadAll() {
        new HashMap<>(modules).keySet().forEach(this::unloadModule);
    }

    public Map<String, ScriptModule> getModules() { return modules; }
}