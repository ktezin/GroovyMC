package me.groovymc.features.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;

        InventoryHolder holder = e.getClickedInventory().getHolder();

        if (holder instanceof GuiHolder) {
            e.setCancelled(true);

            GuiHolder gui = (GuiHolder) holder;
            gui.runAction(e.getSlot(), e);
        }
    }
}