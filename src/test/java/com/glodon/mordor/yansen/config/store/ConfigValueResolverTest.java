package com.glodon.mordor.yansen.config.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigValueResolverTest {

    @Test
    void leavesPlainLiteralUnchanged() {
        assertEquals("sk-abc", ConfigValueResolver.resolveStored("sk-abc"));
    }

    @Test
    void resolveStored_appliesDefaultWhenEnvMissing() {
        assertEquals("fallback-key", ConfigValueResolver.resolveStored("${TEST_API_KEY:fallback-key}"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(ConfigValueResolver.resolveStored(null));
    }
}
