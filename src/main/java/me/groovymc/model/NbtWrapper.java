package me.groovymc.model;

import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class NbtWrapper {
    private final JavaPlugin plugin;
    private final Object target;

    public NbtWrapper(JavaPlugin plugin, Object target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void set(String key, Object value) {
        process(container -> {
            NamespacedKey nsKey = new NamespacedKey(plugin, key);

            if (value instanceof String) {
                container.set(nsKey, PersistentDataType.STRING, (String) value);
            } else if (value instanceof Integer) {
                container.set(nsKey, PersistentDataType.INTEGER, (Integer) value);
            } else if (value instanceof Double) {
                container.set(nsKey, PersistentDataType.DOUBLE, (Double) value);
            } else if (value instanceof Long) {
                container.set(nsKey, PersistentDataType.LONG, (Long) value);
            } else if (value instanceof Float) {
                container.set(nsKey, PersistentDataType.FLOAT, (Float) value);
            } else if (value instanceof Boolean) {
                container.set(nsKey, PersistentDataType.BYTE, (byte) ((Boolean) value ? 1 : 0));
            }
        });
    }

    public Object get(String key) {
        return get(key, PersistentDataType.STRING);
    }

    public <T, Z> Z get(String key, PersistentDataType<T, Z> type) {
        PersistentDataContainer container = getContainer();
        if (container == null) return null;

        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        return container.get(nsKey, type);
    }

    public String getString(String key) {
        return get(key, PersistentDataType.STRING);
    }

    public Integer getInt(String key) {
        return get(key, PersistentDataType.INTEGER);
    }

    public Double getDouble(String key) {
        return get(key, PersistentDataType.DOUBLE);
    }

    public Boolean getBool(String key) {
        Byte b = get(key, PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    public boolean has(String key) {
        PersistentDataContainer container = getContainer();
        if (container == null) return false;

        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        return container.has(nsKey, PersistentDataType.STRING)
                || container.has(nsKey, PersistentDataType.INTEGER)
                || container.has(nsKey, PersistentDataType.DOUBLE)
                || container.has(nsKey, PersistentDataType.BYTE);
    }

    public void remove(String key) {
        process(container -> {
            container.remove(new NamespacedKey(plugin, key));
        });
    }

    private PersistentDataContainer getContainer() {
        if (target instanceof ItemStack) {
            ItemStack item = (ItemStack) target;
            if (item.getItemMeta() == null) return null;
            return item.getItemMeta().getPersistentDataContainer();
        } else if (target instanceof Entity) {
            return ((Entity) target).getPersistentDataContainer();
        } else if (target instanceof TileState) {
            return ((TileState) target).getPersistentDataContainer();
        }
        return null;
    }

    private void process(ContainerAction action) {
        if (target instanceof ItemStack) {
            ItemStack item = (ItemStack) target;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            action.apply(meta.getPersistentDataContainer());

            item.setItemMeta(meta);
        } else if (target instanceof Entity) {
            action.apply(((Entity) target).getPersistentDataContainer());
        } else if (target instanceof TileState) {
            TileState block = (TileState) target;
            action.apply(block.getPersistentDataContainer());
            block.update();
        }
    }

    interface ContainerAction {
        void apply(PersistentDataContainer container);
    }
}