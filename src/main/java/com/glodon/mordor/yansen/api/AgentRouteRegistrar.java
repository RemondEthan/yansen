package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.config.store.AgentConfigRecord;
import com.glodon.mordor.yansen.registry.RouteRegistry;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.config.RoutesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Registers POST and SSE endpoints for each agent route from configuration.
 */
public final class AgentRouteRegistrar {

    private static final Logger log = LoggerFactory.getLogger(AgentRouteRegistrar.class);
    private static final String STREAM_SUFFIX = "/stream";

    private final RouteRegistry routeRegistry;
    private final AgentChatHandler chatHandler;
    private final AgentStreamHandler streamHandler;

    public AgentRouteRegistrar(RouteRegistry routeRegistry,
                                 AgentChatHandler chatHandler,
                                 AgentStreamHandler streamHandler) {
        this.routeRegistry = routeRegistry;
        this.chatHandler = chatHandler;
        this.streamHandler = streamHandler;
    }

    public void registerAgents(RoutesConfig routes, List<AgentConfigRecord> agents) {
        Set<String> seenRoutes = new HashSet<>();
        for (AgentConfigRecord agent : agents) {
            String route = agent.route();
            if (!seenRoutes.add(route)) {
                throw new IllegalStateException("Duplicate route in agent configuration: " + route);
            }
            routeRegistry.register(route, agent.agentId());
            registerRouteHandlers(routes, route);
            log.info("Registered agent '{}' at route {} (stream: {}{})",
                    agent.agentId(), route, route, STREAM_SUFFIX);
        }
    }

    private void registerRouteHandlers(RoutesConfig routes, String route) {
        routes.apiBuilder(() -> {
            ApiBuilder.post(route, ctx -> chatHandler.handle(ctx, route));
            ApiBuilder.sse(route + STREAM_SUFFIX, client -> streamHandler.handle(client, route));
        });
    }
}
