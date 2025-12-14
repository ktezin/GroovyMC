package me.groovymc.features.command;

import groovy.lang.Closure;
import me.groovymc.view.MessageView;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ScriptCommand extends Command {
    private final String moduleName;
    private final Closure executor;
    private Closure tabCompleter;

    public ScriptCommand(String name, String moduleName, Closure executor) {
        super(name);
        this.moduleName = moduleName;
        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (executor == null) return false;

        try {
            executor.call(sender, args);
        } catch (Exception e) {
            MessageView.logScriptError(moduleName, e);
            MessageView.sendError(sender, "&cAn error occured while executing this command");;
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if (tabCompleter != null) {
            try {
                Object result = tabCompleter.call(sender, args);

                if (result instanceof List) {
                    return (List<String>) result;
                }
            } catch (Exception e) {
                MessageView.logScriptError(moduleName, e);
                return Collections.emptyList();
            }
        }
        return super.tabComplete(sender, alias, args);
    }

    public ScriptCommand tabComplete(Closure completer) {
        this.tabCompleter = completer;
        return this;
    }
}