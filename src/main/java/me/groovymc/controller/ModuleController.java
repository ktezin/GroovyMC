package me.groovymc.controller;

import me.groovymc.model.ScriptModule;
import me.groovymc.script.GroovyMCBase;
import me.groovymc.view.MessageView;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModuleController {
    private final JavaPlugin plugin;
    private final CommandRegistry commandRegistry;

    private final Map<String, ScriptModule> modules = new HashMap<>();
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final File modulesFolder;

    public ModuleController(JavaPlugin plugin) {
        this.plugin = plugin;

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
                String name = folder.getName();

                if (name.startsWith("_")) continue;

                File main = new File(folder, "main.groovy");

                if (!main.exists()) continue;

                long currentModified = calculateFolderLastModified(folder);

                if (!modules.containsKey(name)) {
                    loadModule(name);
                    fileTimestamps.put(name, currentModified);
                } else {
                    Long lastKnown = fileTimestamps.get(name);
                    if (lastKnown == null || currentModified > lastKnown) {
                        MessageView.log("Changes detected: " + name + ", module reloading...");
                        reloadModule(name);
                        fileTimestamps.put(name, currentModified);
                    }
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
            MessageView.log("Module loaded: " + name);
        } catch (Exception e) {
            printScriptError(name, e);
            module.cleanup();
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

    private void printScriptError(String moduleName, Exception e) {
        MessageView.logError("&c---------------------------------------------");
        MessageView.logError("&c[ERROR] Module '" + moduleName + "' could not be loaded!");

        if (e instanceof MultipleCompilationErrorsException) {
            MessageView.logError("&eReason: &fSyntax Error");

            String msg = e.getMessage();
            if (msg.contains("startup failed:")) msg = msg.replace("startup failed:", "").trim();

            MessageView.logError("&7Details: " + msg);
        }
        else {
            MessageView.logError("&eReason: &f" + e.getClass().getSimpleName());
            MessageView.logError("&7Message: " + e.getMessage());

            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getFileName() != null && element.getFileName().endsWith(".groovy")) {
                    MessageView.logError("&6Location: &f" + element.getFileName() + " -> Line " + element.getLineNumber());
                    break;
                }
            }
        }
        MessageView.logError("&c---------------------------------------------");
    }

    public Map<String, ScriptModule> getModules() {
        return modules;
    }
}