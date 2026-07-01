package com.glodon.mordor.yansen.agent;

import com.glodon.mordor.yansen.api.UpstreamTimeoutException;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: {@link YansenAgent} backed by a configured {@link HarnessAgent} instance.
 */
public final class YansenAgentImpl implements YansenAgent {

    private static final Logger log = LoggerFactory.getLogger(YansenAgentImpl.class);
    private static final Pattern THINK_TAG =
            Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    private final String agentId;
    private final HarnessAgent agent;
    private final long chatTimeoutSeconds;

    public YansenAgentImpl(String agentId, HarnessAgent agent, long chatTimeoutSeconds) {
        this.agentId = agentId;
        this.agent = agent;
        this.chatTimeoutSeconds = chatTimeoutSeconds;
    }

    @Override
    public String agentId() {
        return agentId;
    }

    @Override
    public String chat(String prompt, String userId, String sessionId) {
        RuntimeContext ctx = runtimeContext(userId, sessionId);
        Msg result = agent.call(new UserMessage(prompt), ctx)
                .blockOptional(Duration.ofSeconds(chatTimeoutSeconds))
                .orElseThrow(() -> new UpstreamTimeoutException(
                        "LLM response timed out after " + chatTimeoutSeconds + "s"));
        logThinking(result, sessionId);
        return extractTextContent(result);
    }

    @Override
    public Flux<AgentEvent> chatStream(String prompt, String userId, String sessionId) {
        return agent.streamEvents(prompt, runtimeContext(userId, sessionId));
    }

    @Override
    public void close() {
        agent.close();
    }

    private void logThinking(Msg msg, String sessionId) {
        for (ThinkingBlock block : msg.getContentBlocks(ThinkingBlock.class)) {
            log.debug("[sessionId={}] thinking: {}", sessionId, block.getThinking());
        }
    }

    private static String extractTextContent(Msg msg) {
        String text = msg.getContentBlocks(TextBlock.class).stream()
                .map(TextBlock::getText)
                .collect(Collectors.joining());
        return THINK_TAG.matcher(text).replaceAll("").trim();
    }

    private static RuntimeContext runtimeContext(String userId, String sessionId) {
        return RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();
    }
}
