package me.groovymc;

import me.groovymc.controller.ModuleController;
import me.groovymc.db.DatabaseManager;
import me.groovymc.view.MessageView;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class GroovyMCPlugin extends JavaPlugin implements CommandExecutor {

    public static DatabaseManager dbManager;
    private ModuleController controller;

    @Override
    public void onEnable() {
        MessageView.init(this);

        dbManager = new DatabaseManager(this);
        this.controller = new ModuleController(this);

        getCommand("groovymc").setExecutor(this);

        controller.loadAll();
    }

    @Override
    public void onDisable() {
        if (controller != null) controller.unloadAll();
        if (dbManager != null) dbManager.close();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("groovymc.admin")) return true;

        if (args.length == 0) {
            MessageView.send(sender, "&eUsage: /groovymc <load|unload|reload|list> [module]");
            return true;
        }

        String action = args[0].toLowerCase();
        String moduleName = args.length > 1 ? args[1] : null;

        switch (action) {
            case "list":
                MessageView.sendSuccess(sender, "Active modules:");
                controller.getModules().keySet().forEach(k -> MessageView.send(sender, " - " + k));
                break;
            case "reload":
                if (moduleName != null) {
                    controller.reloadModule(moduleName);
                    MessageView.sendSuccess(sender, moduleName + " reloaded.");
                } else {
                    controller.unloadAll();
                    controller.loadAll();
                    MessageView.sendSuccess(sender, "All modules reloaded.");
                }
                break;
            case "load":
                if (moduleName != null) controller.loadModule(moduleName);
                break;
            case "unload":
                if (moduleName != null) controller.unloadModule(moduleName);
                break;
        }
        return true;
    }
}