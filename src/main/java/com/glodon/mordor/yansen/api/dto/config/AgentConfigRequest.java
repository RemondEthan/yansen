package com.glodon.mordor.yansen.api.dto.config;

import java.util.List;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Request body for agent config create/update.
 */
public record AgentConfigRequest(
        String agentId,
        String name,
        String agentType,
        String route,
        String modelId,
        Integer systemPromptId,
        String workspace,
        List<String> tools,
        List<String> skills,
        List<String> mcp) {
}
