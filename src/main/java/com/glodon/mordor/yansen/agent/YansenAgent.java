package com.glodon.mordor.yansen.agent;

import io.agentscope.core.event.AgentEvent;
import reactor.core.publisher.Flux;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Chat-capable agent instance backed by HarnessAgent.
 */
public interface YansenAgent extends AutoCloseable {

    String agentId();

    String chat(String prompt, String userId, String sessionId);

    Flux<AgentEvent> chatStream(String prompt, String userId, String sessionId);

    @Override
    void close();
}
