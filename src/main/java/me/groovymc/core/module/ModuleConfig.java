package me.groovymc.core.module;

import groovy.lang.GroovyObjectSupport;
import me.groovymc.view.MessageView;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ModuleConfig extends GroovyObjectSupport {
    private final File file;
    private YamlConfiguration yaml;

    public ModuleConfig(File moduleFolder) {
        this.file = new File(moduleFolder, "config.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                MessageView.logError("Could not create config.yml", e);
            }
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            MessageView.logError("Could not save config.yml", e);
        }
    }

    public void reload() {
        load();
    }


    @Override
    public Object getProperty(String name) {
        if (!yaml.contains(name)) return null;
        Object val = yaml.get(name);

        if (val instanceof ConfigurationSection) {
            return new ConfigSectionMap((ConfigurationSection) val);
        }

        return val;
    }

    @Override
    public void setProperty(String name, Object newValue) {
        yaml.set(name, newValue);
    }

    public void setDefault(String path, Object value) {
        if (!yaml.contains(path)) {
            yaml.set(path, value);
        }
    }

    public YamlConfiguration getYaml() {
        return yaml;
    }

    public static class ConfigSectionMap implements Map<String, Object> {
        private final ConfigurationSection section;

        public ConfigSectionMap(ConfigurationSection section) {
            this.section = section;
        }

        @Override
        public Object get(Object key) {
            if (!(key instanceof String)) return null;
            String path = (String) key;

            if (!section.contains(path)) return null;

            Object val = section.get(path);

            if (val instanceof ConfigurationSection) {
                return new ConfigSectionMap((ConfigurationSection) val);
            }
            return val;
        }

        @Override
        public Object put(String key, Object value) {
            section.set(key, value);
            return value;
        }

        @Override
        public int size() {
            return section.getKeys(false).size();
        }

        @Override
        public boolean isEmpty() {
            return section.getKeys(false).isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return (key instanceof String) && section.contains((String) key);
        }

        @Override
        public boolean containsValue(Object value) {
            return section.getValues(false).containsValue(value);
        }

        @Override
        public Object remove(Object key) {
            Object val = get(key);
            if (key instanceof String) section.set((String) key, null);
            return val;
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            for (Entry<? extends String, ?> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void clear() {
            for (String key : section.getKeys(false)) {
                section.set(key, null);
            }
        }

        @Override
        public Set<String> keySet() {
            return section.getKeys(false);
        }

        @Override
        public Collection<Object> values() {
            return section.getValues(false).values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return section.getValues(false).entrySet();
        }
    }
}