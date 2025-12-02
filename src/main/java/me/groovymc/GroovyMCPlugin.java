package me.groovymc;

import me.groovymc.controller.ModuleController;
import me.groovymc.view.MessageView;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class GroovyMCPlugin extends JavaPlugin implements CommandExecutor {

    private ModuleController controller;
    private MessageView view;

    @Override
    public void onEnable() {
        // MVC Bileşenlerini Başlat
        this.view = new MessageView(this);
        this.controller = new ModuleController(this, view);

        // Komutu kaydet
        getCommand("groovymc").setExecutor(this);

        // Modülleri yükle
        controller.loadAll();
    }

    @Override
    public void onDisable() {
        if (controller != null) {
            controller.unloadAll();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("groovymc.admin")) return true;

        if (args.length == 0) {
            view.send(sender, "&eUsage: /groovymc <load|unload|reload|list> [module]");
            return true;
        }

        String action = args[0].toLowerCase();
        String moduleName = args.length > 1 ? args[1] : null;

        switch (action) {
            case "list":
                view.sendSuccess(sender, "Active modules:");
                controller.getModules().keySet().forEach(k -> view.send(sender, " - " + k));
                break;
            case "reload":
                if (moduleName != null) {
                    controller.reloadModule(moduleName);
                    view.sendSuccess(sender, moduleName + " reloaded.");
                } else {
                    controller.unloadAll();
                    controller.loadAll();
                    view.sendSuccess(sender, "All modules reloaded.");
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