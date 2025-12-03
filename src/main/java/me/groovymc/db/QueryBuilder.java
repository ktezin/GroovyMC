package me.groovymc.db;

import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;
import me.groovymc.view.MessageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryBuilder {
    private final Sql sql;
    private final String table;

    private final List<String> conditions = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();
    private String currentField = null;

    public QueryBuilder(Sql sql, String table) {
        this.sql = sql;
        this.table = table;
    }

    public void add(Map<String, Object> data) {
        try {
            String columns = String.join(", ", data.keySet());
            String placeholders = data.values().stream().map(v -> "?").collect(Collectors.joining(", "));
            String query = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";

            sql.execute(query, new ArrayList<>(data.values()));
        } catch (Exception e) {
            MessageView.logError("Veritabanı ekleme hatası (Tablo: " + table + ")", e);
        }
    }

    public QueryBuilder where(String field) {
        this.currentField = field;
        return this;
    }

    public QueryBuilder is(Object value) {
        conditions.add(currentField + " = ?");
        params.add(value);
        return this;
    }

    public QueryBuilder below(Object value) {
        conditions.add(currentField + " < ?");
        params.add(value);
        return this;
    }


    public QueryBuilder above(Object value) {
        conditions.add(currentField + " > ?");
        params.add(value);
        return this;
    }

    public QueryBuilder between(Object min, Object max) {
        conditions.add(currentField + " >= ? AND " + currentField + " <= ?");
        params.add(min);
        params.add(max);
        return this;
    }

    public List<GroovyRowResult> toArray() {
        String query = "SELECT * FROM " + table + buildWhere();
        try {
            return sql.rows(query, params);
        } catch (Exception e) {
            MessageView.logError("Veri çekme hatası (Query: " + query + ")", e);
            return Collections.emptyList(); // Hata varsa boş liste dön
        }
    }

    public GroovyRowResult first() {
        String query = "SELECT * FROM " + table + buildWhere() + " LIMIT 1";
        try {
            return sql.firstRow(query, params);
        } catch (Exception e) {
            MessageView.logError("Veri çekme hatası (Query: " + query + ")", e);
            return null; // Hata varsa null dön
        }
    }

    public int count() {
        String query = "SELECT COUNT(*) as cnt FROM " + table + buildWhere();
        try {
            GroovyRowResult res = sql.firstRow(query, params);
            return res != null ? ((Number) res.get("cnt")).intValue() : 0;
        } catch (Exception e) {
            MessageView.logError("Sayma hatası (Query: " + query + ")", e);
            return 0; // Hata varsa 0 dön
        }
    }

    public int modify(Map<String, Object> changes) {
        if (changes.isEmpty()) return 0;

        try {
            String setClause = changes.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(", "));
            List<Object> updateParams = new ArrayList<>(changes.values());
            updateParams.addAll(params);

            String query = "UPDATE " + table + " SET " + setClause + buildWhere();
            return sql.executeUpdate(query, updateParams);
        } catch (Exception e) {
            MessageView.logError("Güncelleme hatası (Tablo: " + table + ")", e);
            return 0; // Hata varsa 0 satır güncellendi dön
        }
    }

    public int delete() {
        String query = "DELETE FROM " + table + buildWhere();
        try {
            return sql.executeUpdate(query, params);
        } catch (Exception e) {
            MessageView.logError("Silme hatası (Tablo: " + table + ")", e);
            return 0;
        }
    }

    private String buildWhere() {
        if (conditions.isEmpty()) return "";
        return " WHERE " + String.join(" AND ", conditions);
    }
}