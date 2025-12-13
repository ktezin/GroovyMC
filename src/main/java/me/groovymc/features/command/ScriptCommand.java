package me.groovymc.features.command;

import groovy.lang.Closure;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ScriptCommand extends Command {
    private final Closure executor;
    private Closure tabCompleter;

    public ScriptCommand(String name, Closure executor) {
        super(name);
        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (executor != null) {
            executor.call(sender, args);
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
                e.printStackTrace();
            }
        }
        return super.tabComplete(sender, alias, args);
    }

    public ScriptCommand tabComplete(Closure completer) {
        this.tabCompleter = completer;
        return this;
    }
}