package com.glodon.mordor.yansen;

import com.glodon.mordor.yansen.config.YansenConfig;
import com.glodon.mordor.yansen.config.YansenSettings;
import com.glodon.mordor.yansen.llm.ModelRegistry;
import com.glodon.mordor.yansen.mcp.McpRegistry;
import com.glodon.mordor.yansen.skill.SkillRegistry;
import com.glodon.mordor.yansen.tool.ToolRegistry;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Application-scoped wiring of typed {@link YansenSettings} to the four registries.
 * <p>The classpath skill source is pre-registered at bootstrap. Per-agent workspace skill
 * sources are <em>not</em> resolved here — each agent carries its own workspace, so the
 * resolution boundary lives with the agent service via
 * {@code SkillRegistry.resolve(ids, workspacePath)}.</p>
 */
public record AgentContext(
        YansenSettings settings,
        ModelRegistry modelRegistry,
        ToolRegistry toolRegistry,
        SkillRegistry skillRegistry,
        McpRegistry mcpRegistry) {

    public static AgentContext bootstrap() {
        YansenSettings settings = YansenConfig.load();
        ModelRegistry modelRegistry = ModelRegistry.discover();
        ToolRegistry toolRegistry = ToolRegistry.discover();
        SkillRegistry skillRegistry = SkillRegistry.fromClasspathConfig(settings.skills());
        McpRegistry mcpRegistry = McpRegistry.fromSettings(settings.mcpServers());
        return new AgentContext(settings, modelRegistry, toolRegistry, skillRegistry, mcpRegistry);
    }
}
