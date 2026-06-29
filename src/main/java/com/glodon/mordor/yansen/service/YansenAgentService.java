package com.glodon.mordor.yansen.service;

import com.glodon.mordor.yansen.AgentContext;
import com.glodon.mordor.yansen.config.AgentSettings;
import com.glodon.mordor.yansen.config.YansenSettings;
import com.glodon.mordor.yansen.config.ModelSettings;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.UserMessage;
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
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Encapsulates HarnessAgent lifecycle and chat operations.
 * Reads the default agent and its referenced model from the typed {@link YansenSettings}, then
 * resolves its tool/skill/mcp lists through the respective registries on {@link AgentContext}.
 */
public class YansenAgentService {

    private static final Logger log = LoggerFactory.getLogger(YansenAgentService.class);

    private static final String SYSTEM_PROMPT_CLASSPATH = "/system-prompt.md";
    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant.";
    private static final int COMPACTION_TRIGGER_MESSAGES = 30;
    private static final int COMPACTION_KEEP_MESSAGES = 10;

    private static final Pattern THINK_TAG = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    private final HarnessAgent agent;
    private final long chatTimeoutSeconds;

    public YansenAgentService(AgentContext context, long chatTimeoutSeconds) {
        YansenSettings settings = context.settings();
        AgentSettings agentSettings = settings.defaultAgent();
        ModelSettings modelSettings = settings.requireModel(agentSettings.model());

        String workspace = agentSettings.workspace() == null ? "" : agentSettings.workspace();
        String systemPrompt = loadSystemPrompt(agentSettings.systemPromptPath());

        Model model = context.modelRegistry().create(modelSettings);
        Toolkit toolkit = context.toolRegistry().buildToolkit(agentSettings.tools());
        List<AgentSkillRepository> skillRepos =
                context.skillRegistry().resolve(agentSettings.skills(), workspace);
        Map<String, McpServerConfig> mcpServers = context.mcpRegistry().resolve(agentSettings.mcp());

        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name(YansenSettings.DEFAULT_AGENT_ID)
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

        if (!agentSettings.tools().isEmpty()) {
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

        this.agent = builder.build();
        this.chatTimeoutSeconds = chatTimeoutSeconds;
    }

    public String chat(String prompt, String userId, String sessionId) {
        RuntimeContext ctx = buildRuntimeContext(userId, sessionId);
        Msg result = agent.call(new UserMessage(prompt), ctx)
                .blockOptional(java.time.Duration.ofSeconds(chatTimeoutSeconds))
                .orElseThrow(() -> new com.glodon.mordor.yansen.api.UpstreamTimeoutException(
                        "LLM response timed out after " + chatTimeoutSeconds + "s"));
        logThinking(result, sessionId);
        return extractTextContent(result);
    }

    public Flux<AgentEvent> chatStream(String prompt, String userId, String sessionId) {
        RuntimeContext ctx = buildRuntimeContext(userId, sessionId);
        return agent.streamEvents(prompt, ctx);
    }

    private void logThinking(Msg msg, String sessionId) {
        for (ThinkingBlock block : msg.getContentBlocks(ThinkingBlock.class)) {
            log.debug("[sessionId={}] thinking: {}", sessionId, block.getThinking());
        }
    }

    private String extractTextContent(Msg msg) {
        String text = msg.getContentBlocks(TextBlock.class).stream()
                .map(TextBlock::getText)
                .collect(Collectors.joining());
        return THINK_TAG.matcher(text).replaceAll("").trim();
    }

    private RuntimeContext buildRuntimeContext(String userId, String sessionId) {
        return RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();
    }

    private static ExecutionConfig buildExecutionConfig(ModelSettings settings) {
        if (settings.maxRetries() == null) return null;
        return ExecutionConfig.builder()
                .maxAttempts(settings.maxRetries())
                .build();
    }

    private static String loadSystemPrompt(String externalPath) {
        if (externalPath != null && !externalPath.isBlank()) {
            try {
                return Files.readString(Path.of(externalPath));
            } catch (IOException e) {
                log.warn("Failed to read external system prompt '{}': {}", externalPath, e.getMessage());
            }
        }
        try (InputStream is = YansenAgentService.class.getResourceAsStream(SYSTEM_PROMPT_CLASSPATH)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to read classpath system prompt: {}", e.getMessage());
        }
        return DEFAULT_SYSTEM_PROMPT;
    }
}
