package me.groovymc;

import me.groovymc.core.module.ModuleManager;
import me.groovymc.features.gui.GuiListener;
import me.groovymc.core.workspace.WorkspaceManager;
import me.groovymc.features.db.DatabaseManager;
import me.groovymc.view.MessageView;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class GroovyMC extends JavaPlugin implements CommandExecutor {

    public static DatabaseManager dbManager;
    private WorkspaceManager workspaceManager;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        dbManager = new DatabaseManager(this);
        this.moduleManager = new ModuleManager(this);
        this.workspaceManager = new WorkspaceManager(this);

        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getCommand("groovymc").setExecutor(this);

        moduleManager.loadAll();
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) moduleManager.unloadAll();
        if (dbManager != null) dbManager.close();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("groovymc.admin")) return true;

        if (args.length == 0) {
            MessageView.send(sender, "&eList of commands for GroovyMC");
            MessageView.send(sender, "&e- /groovymc <load|unload|reload|enable|disable|debug> [module]");
            MessageView.send(sender, "&e- /groovymc create <module_name>");
            MessageView.send(sender, "&e- /groovymc list");
            return true;
        }

        String action = args[0].toLowerCase();
        String moduleName = args.length > 1 ? args[1] : null;

        switch (action) {
            case "create":
                if (moduleName == null) {
                    MessageView.sendError(sender, "Usage: /gmc create <module_name>");
                    return true;
                }
                if (moduleManager.createModule(moduleName.toLowerCase())) {
                    MessageView.sendSuccess(sender, "Module &e" + moduleName + " &ais created!");
                } else {
                    MessageView.sendError(sender, "Module cannot be created!");
                }
                break;
            case "list":
                MessageView.sendSuccess(sender, "Active modules:");
                moduleManager.getModules().keySet().forEach(k -> MessageView.send(sender, " - " + k));
                break;
            case "reload":
                workspaceManager.setupWorkspace();
                if (moduleName != null) {
                    moduleManager.reloadModule(moduleName);
                    MessageView.sendSuccess(sender, moduleName + " reloaded.");
                } else {
                    moduleManager.unloadAll();
                    moduleManager.loadAll();
                    MessageView.sendSuccess(sender, "All modules and workspace reloaded.");
                }
                break;
            case "load":
                if (moduleName != null) moduleManager.loadModule(moduleName);
                break;
            case "unload":
                if (moduleName != null) moduleManager.unloadModule(moduleName);
                break;
            case "disable":
                if (moduleName != null) {
                    if (moduleManager.toggleModuleState(moduleName, false)) {
                        MessageView.sendSuccess(sender, moduleName + " disabled.");
                    } else {
                        MessageView.sendError(sender, "The module was either not found or not loaded.");
                    }
                }
                break;
            case "enable":
                if (moduleName != null) {
                    String target = moduleName.startsWith("_") ? moduleName.substring(1) : moduleName;

                    if (moduleManager.toggleModuleState(target, true)) {
                        MessageView.sendSuccess(sender, target + " activated.");
                    } else {
                        MessageView.sendError(sender, "No such disabled module was found.");
                    }
                }
                break;

            case "debug":
                if (moduleName != null) {
                    moduleManager.toggleDebug(moduleName);
                }
                break;
        }
        return true;
    }
}