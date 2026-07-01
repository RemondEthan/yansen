package com.glodon.mordor.yansen;

import com.glodon.mordor.yansen.config.ServerSettings;
import com.glodon.mordor.yansen.config.YansenConfig;
import com.glodon.mordor.yansen.config.YansenSettings;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import com.glodon.mordor.yansen.llm.ModelRegistry;
import com.glodon.mordor.yansen.mcp.McpRegistry;
import com.glodon.mordor.yansen.skill.SkillRegistry;
import com.glodon.mordor.yansen.tool.ToolRegistry;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Application-scoped wiring: server settings, config database, and runtime registries.
 */
public record AgentContext(
        ServerSettings server,
        ConfigStore configStore,
        ModelRegistry modelRegistry,
        ToolRegistry toolRegistry,
        SkillRegistry skillRegistry,
        McpRegistry mcpRegistry) {

    public static AgentContext bootstrap(YansenSettings settings, ConfigStore configStore) {
        ServerSettings server = settings.serverOrDefault();
        ModelRegistry modelRegistry = ModelRegistry.discover();
        ToolRegistry toolRegistry = ToolRegistry.discover();
        SkillRegistry skillRegistry = SkillRegistry.fromSkillRecords(configStore.listSkills());
        McpRegistry mcpRegistry = McpRegistry.fromMcpRecords(configStore.listMcp());
        return new AgentContext(server, configStore, modelRegistry, toolRegistry, skillRegistry, mcpRegistry);
    }

    /** Convenience bootstrap: load YAML then open the config database. */
    public static AgentContext bootstrap(ConfigStore configStore) {
        return bootstrap(YansenConfig.load(), configStore);
    }
}
