package com.glodon.mordor.yansen.config.store;

import com.glodon.mordor.yansen.config.PlaceholderResolver;

/**
 * @author: Remond
 * @date: 2026-07-03
 * @description: Resolves {@code ${ENV_VAR:default}} placeholders in values read from the config database.
 * Used only when building a runtime agent instance — stored values are never rewritten on read.
 * Plain literals (no {@code ${...}}) are returned unchanged.
 */
public final class ConfigValueResolver {

    private ConfigValueResolver() {
    }

    /**
     * Resolve placeholders in a stored config string using current environment variables.
     *
     * @param stored value as persisted in SQLite (literal or placeholder expression)
     * @return resolved value for runtime use, or {@code null} if stored was null
     */
    public static String resolveStored(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.contains("${")) {
            return stored;
        }
        return PlaceholderResolver.resolve(stored);
    }
}
