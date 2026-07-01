package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: MySQL connection settings for the configuration database (future use).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MySqlSettings(
        String jdbcUrl,
        String username,
        String password,
        Integer maximumPoolSize) {

    public int maximumPoolSizeOrDefault() {
        return maximumPoolSize == null || maximumPoolSize <= 0 ? 10 : maximumPoolSize;
    }
}
