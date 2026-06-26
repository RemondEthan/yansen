package com.glodon.mordor.yansen.mcp;

import io.agentscope.harness.agent.tools.McpServerConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpRegistryTest {

    private static McpServerConfig stub(String transport) {
        McpServerConfig c = new McpServerConfig();
        c.setTransport(transport);
        return c;
    }

    @Test
    void fromSettingsReturnsRegistry() {
        McpRegistry registry = McpRegistry.fromSettings(Map.of("github", stub("stdio")));
        assertTrue(registry.has("github"));
        assertEquals(1, registry.registeredServers().size());
    }

    @Test
    void fromSettingsWithNullReturnsEmptyRegistry() {
        McpRegistry registry = McpRegistry.fromSettings(null);
        assertEquals(0, registry.registeredServers().size());
    }

    @Test
    void hasReturnsFalseForUnknown() {
        McpRegistry registry = McpRegistry.fromSettings(Map.of("github", stub("stdio")));
        assertFalse(registry.has("nope"));
    }

    @Test
    void resolveReturnsMatchingConfigs() {
        McpServerConfig github = stub("stdio");
        McpServerConfig notion = stub("sse");
        McpRegistry registry = McpRegistry.fromSettings(
                new LinkedHashMap<>(Map.of("github", github, "notion", notion)));

        Map<String, McpServerConfig> out = registry.resolve(List.of("notion", "github"));
        assertEquals(2, out.size());
        assertSame(notion, out.get("notion"));
        assertSame(github, out.get("github"));
    }

    @Test
    void resolveEmptyReturnsEmptyMap() {
        McpRegistry registry = McpRegistry.fromSettings(Map.of("github", stub("stdio")));
        assertEquals(0, registry.resolve(List.of()).size());
    }

    @Test
    void resolveNullReturnsEmptyMap() {
        McpRegistry registry = McpRegistry.fromSettings(Map.of("github", stub("stdio")));
        assertEquals(0, registry.resolve(null).size());
    }

    @Test
    void resolveThrowsForUnknownId() {
        McpRegistry registry = McpRegistry.fromSettings(Map.of("github", stub("stdio")));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registry.resolve(List.of("nope")));
        assertTrue(e.getMessage().contains("nope"));
        assertTrue(e.getMessage().contains("github"));
    }

    @Test
    void resolveResultIsUnmodifiable() {
        McpRegistry registry = McpRegistry.fromSettings(Map.of("github", stub("stdio")));
        Map<String, McpServerConfig> out = registry.resolve(List.of("github"));
        assertThrows(UnsupportedOperationException.class, () -> out.put("rogue", stub("stdio")));
    }

    @Test
    void rejectsNullMap() {
        assertThrows(NullPointerException.class, () -> new McpRegistry(null));
    }
}
