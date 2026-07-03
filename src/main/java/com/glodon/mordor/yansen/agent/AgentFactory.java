package com.glodon.mordor.yansen.agent;

import com.glodon.mordor.yansen.AgentContext;
import com.glodon.mordor.yansen.config.ModelSettings;
import com.glodon.mordor.yansen.config.store.AgentConfigRecord;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import com.glodon.mordor.yansen.config.store.ModelConfigRecord;
import com.glodon.mordor.yansen.config.store.ConfigValueResolver;
import com.glodon.mordor.yansen.config.store.SystemPromptRecord;
import com.glodon.mordor.yansen.config.store.ToolConfigRecord;
import com.glodon.mordor.yansen.skill.SkillRegistry;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.tools.McpServerConfig;
import io.agentscope.harness.agent.tools.ToolsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Builds {@link YansenAgent} instances from {@link AgentConfigRecord} and SQLite config.
 */
public final class AgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);

    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant.";
    private static final int COMPACTION_TRIGGER_MESSAGES = 30;
    private static final int COMPACTION_KEEP_MESSAGES = 10;

    private final AgentContext context;
    private final ConfigStore configStore;
    private final long chatTimeoutSeconds;

    public AgentFactory(AgentContext context, ConfigStore configStore, long chatTimeoutSeconds) {
        this.context = context;
        this.configStore = configStore;
        this.chatTimeoutSeconds = chatTimeoutSeconds;
    }

    public YansenAgent create(AgentConfigRecord record) {
        ModelConfigRecord modelRecord = configStore.getModel(record.modelId())
                .orElseThrow(() -> new AgentNotFoundException(
                        "model '" + record.modelId() + "' not found for agent '" + record.agentId() + "'"));

        String workspace = ConfigValueResolver.resolveStored(record.workspace());
        if (workspace == null || workspace.isBlank()) {
            throw new IllegalArgumentException("workspace path must not be blank");
        }
        String systemPrompt = resolveSystemPrompt(record);

        ModelSettings modelSettings = modelRecord.toModelSettings();
        Model model = context.modelRegistry().create(modelSettings);
        List<String> enabledToolIds = resolveEnabledToolIds(record);
        Toolkit toolkit = context.toolRegistry().buildToolkit(enabledToolIds);
        List<AgentSkillRepository> skillRepos =
                context.skillRegistry().resolve(resolveSkillSourceIds(record), workspace);
        Map<String, McpServerConfig> mcpServers = context.mcpRegistry().resolve(record.mcpIds());

        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name(record.agentId())
                .workspace(workspace)
                .model(model)
                .sysPrompt(systemPrompt)
                .enablePendingToolRecovery(true)
                .compaction(CompactionConfig.builder()
                        .triggerMessages(COMPACTION_TRIGGER_MESSAGES)
                        .keepMessages(COMPACTION_KEEP_MESSAGES)
                        .build());

        ExecutionConfig execConfig = buildExecutionConfig(modelSettings);
        if (execConfig != null) {
            builder.modelExecutionConfig(execConfig);
        }

        if (!enabledToolIds.isEmpty()) {
            builder.toolkit(toolkit);
        }
        if (!skillRepos.isEmpty()) {
            builder.skillRepositories(skillRepos);
        }
        if (!mcpServers.isEmpty()) {
            ToolsConfig toolsConfig = new ToolsConfig();
            toolsConfig.setMcpServers(mcpServers);
            builder.toolsConfig(toolsConfig);
        }

        log.info("Instantiated agent '{}' type={} route={}", record.agentId(), record.agentType(), record.route());
        return new YansenAgentImpl(record.agentId(), builder.build(), chatTimeoutSeconds);
    }

    private String resolveSystemPrompt(AgentConfigRecord record) {
        Integer promptId = record.systemPromptId();
        if (promptId == null) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return configStore.getPrompt(promptId)
                .map(this::resolvePromptForRuntime)
                .orElseGet(() -> {
                    log.warn("system prompt id {} not found for agent '{}', using default",
                            promptId, record.agentId());
                    return DEFAULT_SYSTEM_PROMPT;
                });
    }

    private String resolvePromptForRuntime(SystemPromptRecord prompt) {
        return switch (prompt.sourceType()) {
            case "inline" -> ConfigValueResolver.resolveStored(prompt.sourceRef());
            case "file" -> readPromptFile(ConfigValueResolver.resolveStored(prompt.sourceRef()));
            case "classpath" -> ConfigValueResolver.resolveStored(configStore.resolvePromptContent(prompt));
            default -> throw new IllegalArgumentException("Unknown source type: " + prompt.sourceType());
        };
    }

    private static String readPromptFile(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt file: " + path, e);
        }
    }

    private List<String> resolveEnabledToolIds(AgentConfigRecord record) {
        return record.toolIds().stream()
                .flatMap(toolId -> configStore.getTool(toolId).stream())
                .filter(ToolConfigRecord::enabled)
                .map(ToolConfigRecord::toolId)
                .toList();
    }

    private List<String> resolveSkillSourceIds(AgentConfigRecord record) {
        return record.skillIds().stream()
                .map(skillId -> configStore.getSkill(skillId)
                        .map(SkillRegistry::toResolveId)
                        .orElseThrow(() -> new AgentNotFoundException(
                                "skill '" + skillId + "' not found for agent '" + record.agentId() + "'")))
                .toList();
    }

    private static ExecutionConfig buildExecutionConfig(ModelSettings settings) {
        if (settings.maxRetries() == null) {
            return null;
        }
        return ExecutionConfig.builder()
                .maxAttempts(settings.maxRetries())
                .build();
    }
}
