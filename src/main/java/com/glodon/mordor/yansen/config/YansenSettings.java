package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.agentscope.harness.agent.tools.McpServerConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Root typed configuration. Holds server, model/agent registries, skill discovery
 * config, and MCP server definitions. Map fields are normalized to immutable in the canonical ctor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YansenSettings(
        ServerSettings server,
        Map<String, ModelSettings> models,
        Map<String, AgentSettings> agents,
        SkillsConfig skills,
        Map<String, McpServerConfig> mcpServers) {

    public static final String DEFAULT_AGENT_ID = "default";

    public YansenSettings {
        models = models == null ? Map.of() : Map.copyOf(models);
        agents = agents == null ? Map.of() : Map.copyOf(agents);
        mcpServers = mcpServers == null ? Map.of() : Map.copyOf(mcpServers);
    }

    public ServerSettings serverOrDefault() {
        return server == null ? new ServerSettings(null, null, null, null) : server;
    }

    public ModelSettings requireModel(String id) {
        ModelSettings m = models.get(id);
        if (m == null) {
            throw new IllegalStateException("model '" + id + "' is not defined in settings.models");
        }
        return m;
    }

    public AgentSettings requireAgent(String id) {
        AgentSettings a = agents.get(id);
        if (a == null) {
            throw new IllegalStateException("agent '" + id + "' is not defined in settings.agents");
        }
        return a;
    }

    public AgentSettings defaultAgent() {
        return requireAgent(DEFAULT_AGENT_ID);
    }

    /**
     * Field-wise merge: non-null entries from {@code override} win over {@code this}.
     * Maps merge per-key (override keys replace base keys). Records are replaced wholesale.
     */
    public YansenSettings mergedWith(YansenSettings override) {
        if (override == null) {
            return this;
        }
        ServerSettings mergedServer = override.server != null ? override.server : this.server;
        SkillsConfig mergedSkills = override.skills != null ? override.skills : this.skills;
        Map<String, ModelSettings> mergedModels = mergeMaps(this.models, override.models);
        Map<String, AgentSettings> mergedAgents = mergeMaps(this.agents, override.agents);
        Map<String, McpServerConfig> mergedMcpServers = mergeMaps(this.mcpServers, override.mcpServers);
        return new YansenSettings(mergedServer, mergedModels, mergedAgents, mergedSkills, mergedMcpServers);
    }

    private static <V> Map<String, V> mergeMaps(Map<String, V> base, Map<String, V> override) {
        if (override == null || override.isEmpty()) {
            return base == null ? Map.of() : base;
        }
        Map<String, V> result = new LinkedHashMap<>();
        if (base != null) {
            result.putAll(base);
        }
        result.putAll(override);
        return result;
    }
}
