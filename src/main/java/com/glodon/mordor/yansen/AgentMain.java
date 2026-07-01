package com.glodon.mordor.yansen;

import com.glodon.mordor.yansen.agent.AgentFactory;
import com.glodon.mordor.yansen.api.ApiModule;
import com.glodon.mordor.yansen.config.ServerSettings;
import com.glodon.mordor.yansen.config.YansenConfig;
import com.glodon.mordor.yansen.config.YansenSettings;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import com.glodon.mordor.yansen.config.store.ConfigStoreFactory;
import com.glodon.mordor.yansen.registry.AgentRegistry;
import com.glodon.mordor.yansen.registry.RouteRegistry;
import io.javalin.Javalin;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Yansen agent server entry point.
 */
public class AgentMain {

    private static final Logger log = LoggerFactory.getLogger(AgentMain.class);

    private static final String SLF4J_PACKAGE_LEVEL_PROPERTY =
            "org.slf4j.simpleLogger.log.com.glodon.mordor.yansen";

    public static void main(String[] args) {
        applyLogLevelFromEnv();

        YansenSettings settings = YansenConfig.load();
        ServerSettings serverSettings = settings.serverOrDefault();
        String configDbPath = settings.databaseOrDefault().sqliteOrDefault().pathOrDefault();

        ConfigStore configStore = ConfigStoreFactory.open(settings.databaseOrDefault());
        AgentContext context = AgentContext.bootstrap(settings, configStore);

        RouteRegistry routeRegistry = new RouteRegistry();
        AgentFactory agentFactory = new AgentFactory(
                context, configStore, serverSettings.chatTimeoutSecondsOrDefault());
        AgentRegistry agentRegistry = new AgentRegistry(configStore, agentFactory);

        Javalin app = Javalin.create(c -> {
            c.http.maxRequestSize = serverSettings.maxRequestSizeBytesOrDefault();
            configureJettyForSse(c, serverSettings);
        });

        ApiModule apiModule = ApiModule.configure(
                app.unsafe.routes, configStore, routeRegistry, agentRegistry, serverSettings, configDbPath);

        int port = serverSettings.portOrDefault();
        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                app.stop();
            } finally {
                apiModule.shutdown();
            }
        }, "yansen-shutdown"));

        log.info("Yansen agent server started on port {} configDb={} routes={} (log level: {})",
                port, configDbPath, routeRegistry.snapshot().size(),
                System.getProperty(SLF4J_PACKAGE_LEVEL_PROPERTY, "info"));
    }

    private static void applyLogLevelFromEnv() {
        String level = System.getenv("YANSEN_LOG_LEVEL");
        if (level != null && !level.isBlank()) {
            System.setProperty(SLF4J_PACKAGE_LEVEL_PROPERTY, level.trim().toLowerCase());
        }
    }

    private static void configureJettyForSse(io.javalin.config.JavalinConfig c, ServerSettings settings) {
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
