package com.glodon.mordor.yansen.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlaceholderResolverTest {

    private static Function<String, String> env(Map<String, String> entries) {
        return entries::get;
    }

    @Test
    void resolvesPlaceholderWithDefault() {
        assertEquals("fallback", PlaceholderResolver.resolve("${MISSING:fallback}", env(Map.of())));
    }

    @Test
    void resolvesPlaceholderFromLookup() {
        assertEquals("hello", PlaceholderResolver.resolve("${GREETING:hi}", env(Map.of("GREETING", "hello"))));
    }

    @Test
    void lookupOverridesDefault() {
        assertEquals("env-value", PlaceholderResolver.resolve("${VAR:fallback}", env(Map.of("VAR", "env-value"))));
    }

    @Test
    void leavesPlainStringUntouched() {
        assertEquals("no-placeholder", PlaceholderResolver.resolve("no-placeholder", env(Map.of())));
    }

    @Test
    void resolvesMultiplePlaceholdersInOneString() {
        Function<String, String> lookup = name -> switch (name) {
            case "A" -> "alpha";
            case "B" -> "beta";
            default -> null;
        };
        assertEquals("alpha and beta", PlaceholderResolver.resolve("${A} and ${B}", lookup));
    }

    @Test
    void supportsEmptyDefault() {
        assertEquals("", PlaceholderResolver.resolve("${MISSING:}", env(Map.of())));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(PlaceholderResolver.resolve(null, env(Map.of())));
    }

    @Test
    void returnsEmptyForEmptyInput() {
        assertEquals("", PlaceholderResolver.resolve("", env(Map.of())));
    }

    @Test
    void escapesRegexSpecialCharsInReplacement() {
        // Without proper escaping, "$1.00" would be interpreted by appendReplacement.
        String result = PlaceholderResolver.resolve("${PRICE:default}", env(Map.of("PRICE", "$1.00")));
        assertEquals("$1.00", result);
    }
}
