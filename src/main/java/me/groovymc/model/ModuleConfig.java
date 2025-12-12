package me.groovymc.model;

import groovy.lang.GroovyObjectSupport;
import me.groovymc.view.MessageView;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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
        return convertToMap(val);
    }

    private Object convertToMap(Object obj) {
        if (obj instanceof ConfigurationSection) {
            Map<String, Object> map = new LinkedHashMap<>();
            Map<String, Object> original = ((ConfigurationSection) obj).getValues(false);

            for (Map.Entry<String, Object> entry : original.entrySet()) {
                map.put(entry.getKey(), convertToMap(entry.getValue()));
            }
            return map;
        }
        return obj;
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
}