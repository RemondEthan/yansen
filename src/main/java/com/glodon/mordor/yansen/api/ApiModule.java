package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.config.ServerSettings;
import com.glodon.mordor.yansen.service.YansenAgentService;
import io.javalin.config.JavalinConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Single entry point for HTTP wiring. Called from
 * {@link com.glodon.mordor.yansen.AgentMain#main} inside the {@link Javalin#create} config
 * lambda; assembles the controllers, the SSE mapper, and the global exception mapper in one
 * place so the main class stays free of HTTP details.
 */
public final class ApiModule {

    /**
     * Thread name used for the shared SSE heartbeat executor. Single-threaded because
     * heartbeats are tiny comment writes spaced seconds apart — a queue is fine.
     */
    private static final String HEARTBEAT_THREAD_NAME = "sse-heartbeat";

    private ApiModule() {
    }

    public static void configure(JavalinConfig config, YansenAgentService agentService,
                                 ServerSettings serverSettings) {
        SseEventMapper sseMapper = new SseEventMapper();
        ScheduledExecutorService heartbeatExecutor =
                Executors.newSingleThreadScheduledExecutor(daemonThreadFactory(HEARTBEAT_THREAD_NAME));
        new HealthController().register(config.routes);
        new ChatController(agentService, sseMapper, heartbeatExecutor,
                serverSettings.keepAliveIntervalSecondsOrDefault()).register(config.routes);
        GlobalExceptionMapper.register(config.routes);
    }

    private static ThreadFactory daemonThreadFactory(String name) {
        return runnable -> {
            Thread t = new Thread(runnable, name);
            t.setDaemon(true);
            return t;
        };
    }
}
