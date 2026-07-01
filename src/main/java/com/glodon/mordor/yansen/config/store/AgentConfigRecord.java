package com.glodon.mordor.yansen.config.store;

import java.util.List;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Persistence type for agent_config table, including associated tool/skill/mcp ids
 * loaded from the junction tables.
 */
public record AgentConfigRecord(
        String agentId,
        String name,
        String agentType,
        String route,
        String modelId,
        Integer systemPromptId,
        String workspace,
        List<String> toolIds,
        List<String> skillIds,
        List<String> mcpIds,
        String createdAt,
        String updatedAt) {

    public AgentConfigRecord {
        toolIds = toolIds == null ? List.of() : List.copyOf(toolIds);
        skillIds = skillIds == null ? List.of() : List.copyOf(skillIds);
        mcpIds = mcpIds == null ? List.of() : List.copyOf(mcpIds);
    }
}
