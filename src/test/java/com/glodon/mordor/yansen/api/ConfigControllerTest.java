package com.glodon.mordor.yansen.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glodon.mordor.yansen.AgentContext;
import com.glodon.mordor.yansen.agent.AgentFactory;
import com.glodon.mordor.yansen.config.ServerSettings;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import com.glodon.mordor.yansen.config.store.ModelConfigRecord;
import com.glodon.mordor.yansen.config.store.sqlite.SqliteConfigStore;
import com.glodon.mordor.yansen.registry.AgentRegistry;
import com.glodon.mordor.yansen.registry.RouteRegistry;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigControllerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path tempDir;

    private ConfigStore configStore;
    private ApiModule apiModule;
    private AgentRegistry agentRegistry;
    private Javalin app;

    @BeforeEach
    void setUp() {
        configStore = new SqliteConfigStore(tempDir.resolve("config-api.db").toString());
        AgentContext context = AgentContext.bootstrap(configStore);
        ServerSettings server = context.server();
        RouteRegistry routeRegistry = new RouteRegistry();
        AgentFactory agentFactory = new AgentFactory(context, configStore, server.chatTimeoutSecondsOrDefault());
        agentRegistry = new AgentRegistry(configStore, agentFactory);

        app = Javalin.create();
        apiModule = ApiModule.configure(
                app.unsafe.routes,
                configStore,
                routeRegistry,
                agentRegistry,
                server,
                tempDir.resolve("config-api.db").toString());
    }

    @AfterEach
    void tearDown() {
        if (apiModule != null) {
            apiModule.shutdown();
        }
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void createModel_storesPlaceholderLiterally() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/api/config/model", """
                    {
                      "modelId": "test-model",
                      "provider": "openai-compatible",
                      "modelName": "Test",
                      "baseUrl": "https://api.example.com/v1",
                      "apiKey": "${TEST_KEY:default-key}"
                    }
                    """);

            assertEquals(201, response.code());
            assertTrue(response.body().string().contains("${TEST_KEY:default-key}"));

            var stored = configStore.getModel("test-model").orElseThrow();
            assertEquals("${TEST_KEY:default-key}", stored.apiKey());
        });
    }

    @Test
    void getModel_notFound_returns404() {
        JavalinTest.test(app, (server, client) -> {
            assertEquals(404, client.get("/api/config/model/missing").code());
        });
    }

    @Test
    void deleteModel_whenReferenced_returns409() {
        JavalinTest.test(app, (server, client) -> {
            assertEquals(409, client.delete("/api/config/model/default").code());
            assertTrue(configStore.getModel("default").isPresent());
        });
    }

    @Test
    void createAgent_registersRoute() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            assertEquals(201, client.post("/api/config/agent", """
                    {
                      "agentId": "extra-agent",
                      "name": "Extra agent",
                      "agentType": "default",
                      "route": "/api/extra",
                      "modelId": "default",
                      "systemPromptId": 1,
                      "workspace": "./agentscope",
                      "tools": ["datetime"],
                      "skills": ["default"],
                      "mcp": []
                    }
                    """).code());

            Map<String, Object> health = JSON.readValue(
                    client.get("/api/health").body().string(),
                    new TypeReference<>() {
                    });
            @SuppressWarnings("unchecked")
            Map<String, String> routes = (Map<String, String>) health.get("routes");
            assertEquals("extra-agent", routes.get("/api/extra"));
        });
    }

    @Test
    void deleteAgent_unregistersRoute() {
        JavalinTest.test(app, (server, client) -> {
            client.post("/api/config/agent", """
                    {
                      "agentId": "temp-agent",
                      "name": "Temp",
                      "agentType": "default",
                      "route": "/api/temp",
                      "modelId": "default",
                      "systemPromptId": 1,
                      "workspace": "./agentscope",
                      "tools": [],
                      "skills": [],
                      "mcp": []
                    }
                    """);

            assertEquals(204, client.delete("/api/config/agent/temp-agent").code());
            assertEquals(404, client.post("/api/temp", "{\"prompt\":\"hi\"}").code());
        });
    }

    @Test
    void updateAgent_invalidatesCachedInstance() {
        JavalinTest.test(app, (server, client) -> {
            ModelConfigRecord defaultModel = configStore.getModel("default").orElseThrow();
            configStore.updateModel(new ModelConfigRecord(
                    defaultModel.modelId(),
                    defaultModel.provider(),
                    defaultModel.modelName(),
                    defaultModel.baseUrl(),
                    "literal-test-key",
                    defaultModel.maxRetries(),
                    defaultModel.connectTimeoutSeconds(),
                    defaultModel.readTimeoutSeconds(),
                    defaultModel.writeTimeoutSeconds(),
                    defaultModel.createdAt(),
                    null));

            agentRegistry.get("default");
            assertEquals(1, agentRegistry.cachedCount());

            assertEquals(200, client.put("/api/config/agent/default", """
                    {
                      "name": "Default agent updated",
                      "agentType": "default",
                      "route": "/api/chat",
                      "modelId": "default",
                      "systemPromptId": 1,
                      "workspace": "./agentscope",
                      "tools": ["datetime"],
                      "skills": ["default"],
                      "mcp": []
                    }
                    """).code());
            assertEquals(0, agentRegistry.cachedCount());
        });
    }

    @Test
    void createModel_duplicate_returns409() {
        JavalinTest.test(app, (server, client) -> {
            String body = """
                    {
                      "modelId": "dup",
                      "provider": "openai-compatible",
                      "modelName": "M",
                      "baseUrl": "https://api.example.com/v1",
                      "apiKey": "k"
                    }
                    """;
            assertEquals(201, client.post("/api/config/model", body).code());
            assertEquals(409, client.post("/api/config/model", body).code());
        });
    }

    @Test
    void listModels_includesSeedData() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/config/model");
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("\"modelId\":\"default\""));
        });
    }

    @Test
    void createAgent_withUnknownModel_returns400() {
        JavalinTest.test(app, (server, client) -> {
            assertEquals(400, client.post("/api/config/agent", """
                    {
                      "agentId": "bad",
                      "name": "Bad",
                      "agentType": "default",
                      "route": "/api/bad",
                      "modelId": "no-such-model",
                      "workspace": "./agentscope",
                      "tools": [],
                      "skills": [],
                      "mcp": []
                    }
                    """).code());
            assertFalse(configStore.getAgent("bad").isPresent());
        });
    }
}
