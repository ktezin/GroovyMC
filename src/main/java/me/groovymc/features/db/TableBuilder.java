package me.groovymc.features.db;

import groovy.sql.Sql;
import me.groovymc.view.MessageView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TableBuilder {
    private final Sql sql;
    private final String tableName;
    private final List<String> definitions = new ArrayList<>();

    public TableBuilder(Sql sql, String tableName) {
        this.sql = sql;
        this.tableName = tableName;
    }

    public TableBuilder string(String name, int length) {
        definitions.add(name + " VARCHAR(" + length + ")");
        return this;
    }

    public TableBuilder string(String name) {
        return string(name, 255);
    }

    public TableBuilder integer(String name) {
        definitions.add(name + " INT");
        return this;
    }

    public TableBuilder decimal(String name) {
        definitions.add(name + " DOUBLE");
        return this;
    }

    public TableBuilder bool(String name) {
        definitions.add(name + " BOOLEAN");
        return this;
    }

    public TableBuilder text(String name) {
        definitions.add(name + " TEXT");
        return this;
    }

    public TableBuilder primaryKey(String name) {
        definitions.add(name + " INTEGER PRIMARY KEY AUTOINCREMENT");
        return this;
    }

    public TableBuilder uuidKey(String name) {
        definitions.add(name + " VARCHAR(36) PRIMARY KEY");
        return this;
    }

    public TableBuilder uuid(String name) {
        definitions.add(name + " VARCHAR(36)");
        return this;
    }

    public void create() {
        if (definitions.isEmpty()) return;

        String body = String.join(", ", definitions);
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + body + ")";

        try {
            sql.execute(query);
        } catch (SQLException e) {
            MessageView.logError("Error occured while creating (" + tableName + ") table: ", e);
        }
    }
}