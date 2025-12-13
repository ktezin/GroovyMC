package me.groovymc.features.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class DatabaseManager {
    private final HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        File dbFile = new File(plugin.getDataFolder(), "database.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");

        config.setMaximumPoolSize(10);
        config.setPoolName("GroovyMCPool");

        this.dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Veritabanı havuzu başlatıldı.");
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}