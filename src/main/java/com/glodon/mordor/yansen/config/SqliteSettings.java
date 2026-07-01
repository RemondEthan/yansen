package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite file path for the configuration database. Independent of agent workspace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SqliteSettings(String path) {

    public static final String DEFAULT_PATH = "store/yansen.db";

    public String pathOrDefault() {
        return path == null || path.isBlank() ? DEFAULT_PATH : path.trim();
    }
}
