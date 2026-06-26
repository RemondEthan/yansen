package com.glodon.mordor.yansen.config;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Resolves ${ENV_VAR} and ${ENV_VAR:default} placeholders in a string value.
 * Extracted from the legacy YansenConfig so the same logic can be applied uniformly
 * to any tree (YAML tree, JSON tree, plain string).
 */
public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private PlaceholderResolver() {}

    /**
     * Resolve placeholders using {@link System#getenv(String)} as the lookup source.
     */
    public static String resolve(String value) {
        return resolve(value, System::getenv);
    }

    /**
     * Resolve placeholders using a caller-supplied lookup. Useful for tests.
     */
    public static String resolve(String value, Function<String, String> lookup) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        if (!matcher.find()) {
            return value;
        }
        matcher.reset();
        StringBuilder result = new StringBuilder(value.length());
        while (matcher.find()) {
            String content = matcher.group(1);
            int colon = content.indexOf(':');
            String varName = colon >= 0 ? content.substring(0, colon) : content;
            String fallback = colon >= 0 ? content.substring(colon + 1) : "";
            String resolved = lookup.apply(varName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved != null ? resolved : fallback));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
