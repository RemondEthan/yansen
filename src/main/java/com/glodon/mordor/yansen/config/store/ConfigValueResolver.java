package com.glodon.mordor.yansen.config.store;

import com.glodon.mordor.yansen.config.PlaceholderResolver;

/**
 * @author: Remond
 * @date: 2026-07-03
 * @description: Resolves {@code ${ENV_VAR:default}} placeholders in values read from the config database.
 * Applied to every string field that affects runtime behaviour (model settings, agent workspace,
 * prompt paths/content, skill paths, MCP JSON, HTTP routes, association ids, etc.).
 * <p>Resolution is single-pass: if an env var value itself contains {@code ${...}}, it is not
 * expanded again. Primary keys written to SQLite and CRUD API responses are never rewritten —
 * only values consumed when building agents, routes, or registries are resolved.</p>
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
