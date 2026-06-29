package com.glodon.mordor.yansen.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YansenConfigTest {

    private static YansenSettings parse(String yaml) {
        return YansenConfig.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    private static YansenSettings empty() {
        return new YansenSettings(null, null, null, null, null);
    }

    @Test
    void parsesCompleteYaml() {
        String yaml = """
                server:
                  port: 9090
                  maxRequestSizeBytes: 50000
                models:
                  default:
                    provider: openai-compatible
                    modelName: MiniMax-M3
                    baseUrl: https://api.example.com/v1
                    apiKey: secret-key
                agents:
                  default:
                    model: default
                    workspace: /tmp/work
                    systemPromptPath: /tmp/prompt.md
                    tools: [datetime]
                    skills: [classpath:skills]
                    mcp: []
                skills:
                  workspaceDir: skills
                  classpathBase: skills
                mcp: {}
                """;
        YansenSettings settings = parse(yaml);

        assertEquals(9090, settings.serverOrDefault().portOrDefault());
        assertEquals(50_000L, settings.serverOrDefault().maxRequestSizeBytesOrDefault());

        ModelSettings model = settings.requireModel("default");
        assertEquals("openai-compatible", model.provider());
        assertEquals("MiniMax-M3", model.modelName());
        assertEquals("https://api.example.com/v1", model.baseUrl());
        assertEquals("secret-key", model.apiKey());

        AgentSettings agent = settings.defaultAgent();
        assertEquals("default", agent.model());
        assertEquals("/tmp/work", agent.workspace());
        assertEquals("/tmp/prompt.md", agent.systemPromptPath());
        assertEquals(java.util.List.of("datetime"), agent.tools());
        assertEquals(java.util.List.of("classpath:skills"), agent.skills());
        assertEquals(java.util.List.of(), agent.mcp());

        SkillsConfig skills = settings.skills();
        assertNotNull(skills);
        assertEquals("skills", skills.workspaceDir());
        assertEquals("skills", skills.classpathBase());
    }

    @Test
    void appliesServerDefaultsWhenSectionMissing() {
        String yaml = """
                models:
                  m:
                    provider: openai-compatible
                    modelName: foo
                    apiKey: bar
                agents:
                  default:
                    model: m
                """;
        YansenSettings settings = parse(yaml);

        assertEquals(ServerSettings.DEFAULT_PORT, settings.serverOrDefault().portOrDefault());
        assertEquals(ServerSettings.DEFAULT_MAX_REQUEST_SIZE_BYTES,
                settings.serverOrDefault().maxRequestSizeBytesOrDefault());

        AgentSettings agent = settings.defaultAgent();
        assertEquals("m", agent.model());
        assertNull(agent.workspace());
        assertNull(agent.systemPromptPath());
        assertEquals(java.util.List.of(), agent.tools());
    }

    @Test
    void resolvesPlaceholdersWithDefaultValue() {
        // YANSEN_TEST_PORT is unlikely to be set in CI; default should kick in.
        String yaml = """
                server:
                  port: "${YANSEN_TEST_PORT:7777}"
                """;
        YansenSettings settings = parse(yaml);
        assertEquals(7777, settings.serverOrDefault().portOrDefault());
    }

    @Test
    void mergedWithAddsMissingKeys() {
        YansenSettings base = new YansenSettings(
                new ServerSettings(8080, 10_000L, 15L, null, null, null),
                Map.of("default", new ModelSettings("openai-compatible", "MiniMax-M3", "u", "k",
                        null, null, null, null, null)),
                Map.of("default", new AgentSettings("default", null, null, null, null, null, null)),
                null, null);
        YansenSettings override = new YansenSettings(
                null,
                Map.of("extra", new ModelSettings("openai-compatible", "extra-model", "u2", "k2",
                        null, null, null, null, null)),
                null, null, null);

        YansenSettings merged = base.mergedWith(override);

        assertEquals(2, merged.models().size());
        assertNotNull(merged.requireModel("default"));
        assertNotNull(merged.requireModel("extra"));
        assertEquals(8080, merged.serverOrDefault().portOrDefault());
        assertEquals(1, merged.agents().size());
    }

    @Test
    void mergedWithNullReturnsSelf() {
        YansenSettings base = new YansenSettings(
                new ServerSettings(8080, 10_000L, 15L, null, null, null), Map.of(), Map.of(), null, null);
        assertEquals(base, base.mergedWith(null));
    }

    @Test
    void mergedWithOverridesServer() {
        YansenSettings base = new YansenSettings(
                new ServerSettings(8080, 10_000L, 15L, null, null, null), Map.of(), Map.of(), null, null);
        YansenSettings override = new YansenSettings(
                new ServerSettings(9090, 50_000L, 15L, null, null, null), Map.of(), Map.of(), null, null);

        YansenSettings merged = base.mergedWith(override);

        assertEquals(9090, merged.serverOrDefault().portOrDefault());
        assertEquals(50_000L, merged.serverOrDefault().maxRequestSizeBytesOrDefault());
    }

    @Test
    void mergedWithMergesMcpServers() {
        io.agentscope.harness.agent.tools.McpServerConfig a = new io.agentscope.harness.agent.tools.McpServerConfig();
        a.setTransport("stdio");
        io.agentscope.harness.agent.tools.McpServerConfig b = new io.agentscope.harness.agent.tools.McpServerConfig();
        b.setTransport("sse");

        YansenSettings base = new YansenSettings(null, Map.of(), Map.of(), null, Map.of("a", a));
        YansenSettings override = new YansenSettings(null, Map.of(), Map.of(), null, Map.of("b", b));

        YansenSettings merged = base.mergedWith(override);
        assertEquals(2, merged.mcpServers().size());
        assertNotNull(merged.mcpServers().get("a"));
        assertNotNull(merged.mcpServers().get("b"));
    }

    @Test
    void mergedWithOverridesSkills() {
        YansenSettings base = new YansenSettings(null, Map.of(), Map.of(), new SkillsConfig("a", null), null);
        YansenSettings override = new YansenSettings(null, Map.of(), Map.of(), new SkillsConfig("b", "c"), null);

        YansenSettings merged = base.mergedWith(override);
        assertEquals("b", merged.skills().workspaceDir());
        assertEquals("c", merged.skills().classpathBase());
    }

    @Test
    void requireModelThrowsWhenMissing() {
        YansenSettings settings = empty();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> settings.requireModel("nope"));
        assertTrue(e.getMessage().contains("nope"));
    }

    @Test
    void requireAgentThrowsWhenMissing() {
        YansenSettings settings = empty();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> settings.requireAgent("nope"));
        assertTrue(e.getMessage().contains("nope"));
    }

    @Test
    void defaultAgentThrowsWhenNotConfigured() {
        YansenSettings settings = empty();
        assertThrows(IllegalStateException.class, settings::defaultAgent);
    }
}
