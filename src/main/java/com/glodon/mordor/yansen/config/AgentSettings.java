package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Per-agent configuration.
 * model references a key under YansenSettings#models. tools/skills/mcp reference ids in their
 * respective registries; an empty list means "none" (no implicit default).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentSettings(
        String model,
        String workspace,
        String systemPromptPath,
        String systemPrompt,
        List<String> tools,
        List<String> skills,
        List<String> mcp) {

    public AgentSettings {
        tools = tools == null ? List.of() : List.copyOf(tools);
        skills = skills == null ? List.of() : List.copyOf(skills);
        mcp = mcp == null ? List.of() : List.copyOf(mcp);
    }
}
