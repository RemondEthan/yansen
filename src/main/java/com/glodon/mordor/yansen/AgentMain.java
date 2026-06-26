package com.glodon.mordor.yansen;

import com.glodon.mordor.yansen.api.ApiModule;
import com.glodon.mordor.yansen.config.ServerSettings;
import com.glodon.mordor.yansen.service.YansenAgentService;
import io.javalin.Javalin;
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
            ApiModule.configure(c, agentService, serverSettings);
        });

        int port = serverSettings.portOrDefault();
        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop, "yansen-shutdown"));
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
}
