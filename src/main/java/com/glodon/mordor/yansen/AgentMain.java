package com.glodon.mordor.yansen;

import com.glodon.mordor.yansen.api.ApiModule;
import com.glodon.mordor.yansen.config.ServerSettings;
import com.glodon.mordor.yansen.service.YansenAgentService;
import io.javalin.Javalin;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Yansen agent server entry point. Bootstraps the {@link AgentContext}, builds
 * the agent service, then delegates all HTTP wiring to {@link ApiModule}.
 */
public class AgentMain {

    private static final Logger log = LoggerFactory.getLogger(AgentMain.class);

    /**
     * SLF4J Simple system property that controls the per-package log level. Scoped to
     * our package so that enabling DEBUG doesn't flood Javalin/Jetty with their own
     * debug output. Read once at startup via {@link #applyLogLevelFromEnv()}.
     */
    private static final String SLF4J_PACKAGE_LEVEL_PROPERTY =
            "org.slf4j.simpleLogger.log.com.glodon.mordor.yansen";

    public static void main(String[] args) {
        applyLogLevelFromEnv();
        AgentContext context = AgentContext.bootstrap();
        YansenAgentService agentService = new YansenAgentService(context);
        ServerSettings serverSettings = context.settings().serverOrDefault();

        Javalin app = Javalin.create(c -> {
            c.http.maxRequestSize = serverSettings.maxRequestSizeBytesOrDefault();
            configureJettyForSse(c, serverSettings);
        });
        ApiModule apiModule = ApiModule.configure(app.unsafe.routes, agentService, serverSettings);

        int port = serverSettings.portOrDefault();
        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { app.stop(); } finally { apiModule.shutdown(); }
        }, "yansen-shutdown"));
        log.info("Yansen agent server started on port {} (yansen log level: {})",
                port, System.getProperty(SLF4J_PACKAGE_LEVEL_PROPERTY, "info"));
    }

    /**
     * Reads {@code YANSEN_LOG_LEVEL} from the environment and applies it as the SLF4J
     * Simple log level for the {@code com.glodon.mordor.yansen} package. Must be called
     * before any logger in that package is constructed (i.e., before
     * {@link AgentContext#bootstrap()}).
     */
    private static void applyLogLevelFromEnv() {
        String level = System.getenv("YANSEN_LOG_LEVEL");
        if (level != null && !level.isBlank()) {
            System.setProperty(SLF4J_PACKAGE_LEVEL_PROPERTY, level.trim().toLowerCase());
        }
    }

    /**
     * Raises the Jetty connector idle timeout for SSE streams.
     *
     * <p>Jetty's default HTTP idle timeout is ~30s. During long model thinking/CoT pauses
     * the stream can go silent for longer than that, causing Jetty to close the connection
     * (the "connection closed WITHOUT app completion" WARN). Raising to 5 minutes gives
     * ample margin; the SSE heartbeat (default 10s) keeps the wire active within this
     * window, and each write is followed by {@code flushBuffer()} so bytes reach the
     * TCP socket promptly.
     */
    private static void configureJettyForSse(io.javalin.config.JavalinConfig c,
                                              ServerSettings settings) {
        long idleTimeoutMs = settings.sseIdleTimeoutSecondsOrDefault() * 1000L;
        c.jetty.modifyServer((Server server) -> {
            for (Connector connector : server.getConnectors()) {
                if (connector instanceof AbstractConnector ac) {
                    ac.setIdleTimeout(idleTimeoutMs);
                }
            }
        });
        log.info("Jetty configured for SSE: connector idleTimeout={}s",
                settings.sseIdleTimeoutSecondsOrDefault());
    }
}
