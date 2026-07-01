package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.agent.YansenAgent;
import com.glodon.mordor.yansen.api.dto.ChatRequest;
import com.glodon.mordor.yansen.api.dto.ChatResponse;
import com.glodon.mordor.yansen.registry.AgentRegistry;
import com.glodon.mordor.yansen.registry.RouteRegistry;
import io.javalin.http.Context;

import java.util.UUID;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Synchronous chat handler resolved via {@link RouteRegistry}.
 */
public final class AgentChatHandler {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String DEFAULT_USER_ID = "default-user";

    private final RouteRegistry routeRegistry;
    private final AgentRegistry agentRegistry;

    public AgentChatHandler(RouteRegistry routeRegistry, AgentRegistry agentRegistry) {
        this.routeRegistry = routeRegistry;
        this.agentRegistry = agentRegistry;
    }

    public void handle(Context ctx, String route) {
        ctx.contentType(CONTENT_TYPE_JSON);
        YansenAgent agent = resolveAgent(route);
        ChatRequest req = ctx.bodyAsClass(ChatRequest.class);
        String sessionId = req.sessionId() != null ? req.sessionId() : UUID.randomUUID().toString();
        String userId = req.userId() != null ? req.userId() : DEFAULT_USER_ID;
        String response = agent.chat(req.prompt(), userId, sessionId);
        ctx.json(new ChatResponse(response, sessionId));
    }

    private YansenAgent resolveAgent(String route) {
        String agentId = routeRegistry.resolve(route)
                .orElseThrow(() -> new RouteNotFoundException("No active agent for route: " + route));
        return agentRegistry.get(agentId);
    }
}
