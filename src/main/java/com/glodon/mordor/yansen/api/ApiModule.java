package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.config.ServerSettings;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import com.glodon.mordor.yansen.registry.AgentRegistry;
import com.glodon.mordor.yansen.registry.RouteRegistry;
import io.javalin.config.RoutesConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Single entry point for HTTP wiring: health, dynamic agent routes, exception mapping.
 */
public final class ApiModule {

    private static final Logger log = LoggerFactory.getLogger(ApiModule.class);
    private static final String HEARTBEAT_THREAD_NAME = "sse-heartbeat";

    private final ScheduledExecutorService heartbeatExecutor;
    private final ConfigStore configStore;
    private final AgentRegistry agentRegistry;
    private final RouteRegistry routeRegistry;

    private ApiModule(ScheduledExecutorService heartbeatExecutor,
                      ConfigStore configStore,
                      AgentRegistry agentRegistry,
                      RouteRegistry routeRegistry) {
        this.heartbeatExecutor = heartbeatExecutor;
        this.configStore = configStore;
        this.agentRegistry = agentRegistry;
        this.routeRegistry = routeRegistry;
    }

    public static ApiModule configure(RoutesConfig routes,
                                      ConfigStore configStore,
                                      RouteRegistry routeRegistry,
                                      AgentRegistry agentRegistry,
                                      ServerSettings serverSettings,
                                      String configDbPath) {
        ScheduledExecutorService heartbeatExecutor =
                Executors.newSingleThreadScheduledExecutor(daemonThreadFactory(HEARTBEAT_THREAD_NAME));
        SseEventMapper sseMapper = new SseEventMapper();

        new HealthController(agentRegistry, routeRegistry, configDbPath).registerOn(routes);

        AgentChatHandler chatHandler = new AgentChatHandler(routeRegistry, agentRegistry);
        AgentStreamHandler streamHandler = new AgentStreamHandler(
                routeRegistry, agentRegistry, sseMapper, heartbeatExecutor,
                serverSettings.keepAliveIntervalSecondsOrDefault(),
                serverSettings.sseEventTimeoutSecondsOrDefault());
        new AgentRouteRegistrar(routeRegistry, chatHandler, streamHandler)
                .registerAgents(routes, configStore.listAgents());

        GlobalExceptionMapper.register(routes);
        return new ApiModule(heartbeatExecutor, configStore, agentRegistry, routeRegistry);
    }

    public void shutdown() {
        heartbeatExecutor.shutdownNow();
        agentRegistry.close();
        try {
            configStore.close();
        } catch (Exception e) {
            log.warn("Failed to close config store: {}", e.getMessage());
        }
    }

    private static ThreadFactory daemonThreadFactory(String name) {
        return runnable -> {
            Thread t = new Thread(runnable, name);
            t.setDaemon(true);
            return t;
        };
    }
}
