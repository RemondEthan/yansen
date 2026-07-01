package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Root typed configuration: server infrastructure and database connection only.
 * Business configuration (models, agents, skills, mcp) lives in the config database.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YansenSettings(
        ServerSettings server,
        DatabaseSettings database) {

    public ServerSettings serverOrDefault() {
        return server == null ? new ServerSettings(null, null, null, null, null, null) : server;
    }

    public DatabaseSettings databaseOrDefault() {
        if (database == null) {
            return new DatabaseSettings(null, null, null).databaseOrDefault();
        }
        return database.databaseOrDefault();
    }

    /**
     * Field-wise merge: non-null entries from {@code override} win over {@code this}.
     */
    public YansenSettings mergedWith(YansenSettings override) {
        if (override == null) {
            return this;
        }
        ServerSettings mergedServer = override.server != null ? override.server : this.server;
        DatabaseSettings mergedDatabase = override.database != null ? override.database : this.database;
        return new YansenSettings(mergedServer, mergedDatabase);
    }
}
