package me.groovymc.model;

import groovy.lang.Script;
import org.bukkit.command.Command;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScriptModule {
    private final String name;
    private final File mainFile;
    private final File moduleFolder;
    private Script scriptInstance;
    private boolean debugMode = false;

    private final List<Listener> listeners = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();
    private final List<Integer> taskIds = new ArrayList<>();

    public ScriptModule(String name, File mainFile) {
        this.name = name;
        this.mainFile = mainFile;
        this.moduleFolder = mainFile.getParentFile();
    }

    public void cleanup() {
        listeners.forEach(HandlerList::unregisterAll);
        listeners.clear();

        taskIds.forEach(id -> org.bukkit.Bukkit.getScheduler().cancelTask(id));
        taskIds.clear();

        commands.clear();
    }

    public void addListener(Listener listener) { listeners.add(listener); }
    public void addCommand(Command command) { commands.add(command); }
    public void addTask(int taskId) { taskIds.add(taskId); }

    public List<Command> getCommands() { return new ArrayList<>(commands); }
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public void setScriptInstance(Script script) { this.scriptInstance = script; }
    public Script getScriptInstance() { return scriptInstance; }
    public File getModuleFolder() {
        return moduleFolder;
    }
    public File getMainFile() { return mainFile; }
    public String getName() { return name; }
}