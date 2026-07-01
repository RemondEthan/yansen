package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.agent.YansenAgent;
import com.glodon.mordor.yansen.registry.AgentRegistry;
import com.glodon.mordor.yansen.registry.RouteRegistry;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SSE streaming chat handler resolved via {@link RouteRegistry}.
 */
public final class AgentStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamHandler.class);

    private static final String QUERY_PROMPT = "prompt";
    private static final String QUERY_USER_ID = "userId";
    private static final String QUERY_SESSION_ID = "sessionId";
    private static final String DEFAULT_USER_ID = "default-user";
    private static final String SSE_MISSING_PROMPT = "prompt is required";

    private final RouteRegistry routeRegistry;
    private final AgentRegistry agentRegistry;
    private final SseEventMapper sseMapper;
    private final ScheduledExecutorService heartbeatExecutor;
    private final long keepAliveIntervalSeconds;
    private final long sseEventTimeoutSeconds;

    public AgentStreamHandler(RouteRegistry routeRegistry, AgentRegistry agentRegistry,
                              SseEventMapper sseMapper, ScheduledExecutorService heartbeatExecutor,
                              long keepAliveIntervalSeconds, long sseEventTimeoutSeconds) {
        this.routeRegistry = routeRegistry;
        this.agentRegistry = agentRegistry;
        this.sseMapper = sseMapper;
        this.heartbeatExecutor = heartbeatExecutor;
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
        this.sseEventTimeoutSeconds = sseEventTimeoutSeconds;
    }

    public void handle(SseClient sseClient, String route) {
        Context ctx = sseClient.ctx();
        String prompt = ctx.queryParam(QUERY_PROMPT);
        if (prompt == null || prompt.isBlank()) {
            new SseSession(sseClient, sseMapper, heartbeatExecutor, keepAliveIntervalSeconds,
                    sseEventTimeoutSeconds, "n/a")
                    .sendTerminalError(SSE_MISSING_PROMPT);
            return;
        }

        String userId = ctx.queryParam(QUERY_USER_ID);
        String sessionId = ctx.queryParam(QUERY_SESSION_ID);
        if (userId == null) {
            userId = DEFAULT_USER_ID;
        }
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        final String effectiveSessionId = sessionId;

        log.info("[SSE] open route={} prompt=\"{}\" userId={} sessionId={}",
                route, prompt, userId, effectiveSessionId);

        YansenAgent agent = resolveAgent(route);
        SseSession session = new SseSession(sseClient, sseMapper, heartbeatExecutor,
                keepAliveIntervalSeconds, sseEventTimeoutSeconds, effectiveSessionId);
        session.stream(agent.chatStream(prompt, userId, effectiveSessionId));
    }

    private YansenAgent resolveAgent(String route) {
        String agentId = routeRegistry.resolve(route)
                .orElseThrow(() -> new RouteNotFoundException("No active agent for route: " + route));
        return agentRegistry.get(agentId);
    }
}
