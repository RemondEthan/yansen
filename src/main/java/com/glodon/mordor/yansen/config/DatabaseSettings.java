package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Infrastructure database settings loaded from yansen.yml {@code database} section.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DatabaseSettings(
        String type,
        SqliteSettings sqlite,
        MySqlSettings mysql) {

    public static final String TYPE_SQLITE = "sqlite";
    public static final String TYPE_MYSQL = "mysql";

    public String typeOrDefault() {
        return type == null || type.isBlank() ? TYPE_SQLITE : type.trim().toLowerCase();
    }

    public DatabaseSettings databaseOrDefault() {
        if (sqlite == null && mysql == null && (type == null || type.isBlank())) {
            return new DatabaseSettings(TYPE_SQLITE, new SqliteSettings(null), null);
        }
        return this;
    }

    public SqliteSettings sqliteOrDefault() {
        return sqlite == null ? new SqliteSettings(null) : sqlite;
    }
}
