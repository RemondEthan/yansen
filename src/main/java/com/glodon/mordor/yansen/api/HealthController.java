package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.registry.AgentRegistry;
import com.glodon.mordor.yansen.registry.RouteRegistry;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.RoutesConfig;

import java.util.Map;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Liveness probe and lightweight registry snapshot at {@code GET /api/health}.
 */
public final class HealthController implements Controller {

    private static final String ROUTE = "/api/health";

    private final AgentRegistry agentRegistry;
    private final RouteRegistry routeRegistry;
    private final String configDbPath;

    public HealthController(AgentRegistry agentRegistry, RouteRegistry routeRegistry, String configDbPath) {
        this.agentRegistry = agentRegistry;
        this.routeRegistry = routeRegistry;
        this.configDbPath = configDbPath;
    }

    @Override
    public void registerOn(RoutesConfig routes) {
        routes.apiBuilder((EndpointGroup) () -> ApiBuilder.get(ROUTE, ctx -> ctx.json(buildBody())));
    }

    private HealthBody buildBody() {
        return new HealthBody(
                "ok",
                configDbPath,
                agentRegistry.cachedCount(),
                agentRegistry.listCachedAgentIds(),
                routeRegistry.snapshot());
    }

    public record HealthBody(
            String status,
            String configDbPath,
            int cachedAgents,
            java.util.List<String> cachedAgentIds,
            Map<String, String> routes) {
    }
}
