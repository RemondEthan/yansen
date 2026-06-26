package com.glodon.mordor.yansen.mcp;

import io.agentscope.harness.agent.tools.McpServerConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Maps MCP server ids (as written under agent.mcp in YAML) to {@link McpServerConfig}
 * instances. Pure data wrapper; does not manage connections.
 */
public final class McpRegistry {

    private final Map<String, McpServerConfig> servers;

    public McpRegistry(Map<String, McpServerConfig> servers) {
        Objects.requireNonNull(servers, "servers");
        this.servers = Map.copyOf(servers);
    }

    public boolean has(String id) {
        return servers.containsKey(id);
    }

    public Set<String> registeredServers() {
        return Collections.unmodifiableSet(new TreeSet<>(servers.keySet()));
    }

    /**
     * Resolve a list of MCP server ids to the corresponding configs (preserving order).
     * Empty/null input yields an empty map. Unknown ids throw.
     */
    public Map<String, McpServerConfig> resolve(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<String, McpServerConfig> out = new LinkedHashMap<>();
        for (String id : ids) {
            McpServerConfig cfg = servers.get(id);
            if (cfg == null) {
                throw new IllegalStateException(
                        "No MCP server configured with id '" + id + "'. Configured: " + registeredServers());
            }
            out.put(id, cfg);
        }
        return Collections.unmodifiableMap(out);
    }

    public static McpRegistry fromSettings(Map<String, McpServerConfig> servers) {
        return new McpRegistry(servers == null ? Map.of() : servers);
    }
}
