package me.groovymc.features.db;

import groovy.lang.GroovyObjectSupport;
import groovy.sql.Sql;

public class FluentDatabase extends GroovyObjectSupport {
    private final Sql sql;
    private final String moduleName;
    private final String prefix;

    public FluentDatabase(Sql sql, String moduleName) {
        this.sql = sql;
        this.moduleName = moduleName;
        this.prefix = moduleName.toLowerCase() + "_";
    }

    public TableBuilder define(String tableName) {
        if (tableName.startsWith("global_")) {
            return new TableBuilder(sql, moduleName, tableName);
        }
        return new TableBuilder(sql, moduleName, prefix + tableName);
    }

    public Object getProperty(String name) {
        try {
            return super.getProperty(name);
        } catch (Exception e) {
            String realTableName = name.startsWith("global_") ? name : prefix + name;
            return new QueryBuilder(sql, moduleName, realTableName);
        }
    }

    public Sql getSql() {
        return sql;
    }
}