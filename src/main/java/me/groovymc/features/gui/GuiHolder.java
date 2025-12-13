package me.groovymc.features.gui;

import groovy.lang.Closure;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class GuiHolder implements InventoryHolder {
    private final Map<Integer, Closure> actions = new HashMap<>();
    private Inventory inventory;

    public void setAction(int slot, Closure action) {
        actions.put(slot, action);
    }

    public void runAction(int slot, Object... args) {
        if (actions.containsKey(slot)) {
            actions.get(slot).call(args);
        }
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}