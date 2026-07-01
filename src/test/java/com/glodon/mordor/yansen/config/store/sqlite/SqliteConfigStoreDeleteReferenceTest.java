package com.glodon.mordor.yansen.config.store.sqlite;

import com.glodon.mordor.yansen.config.store.ConfigReferenceException;
import com.glodon.mordor.yansen.config.store.McpConfigRecord;
import com.glodon.mordor.yansen.config.store.ModelConfigRecord;
import com.glodon.mordor.yansen.config.store.SkillConfigRecord;
import com.glodon.mordor.yansen.config.store.SystemPromptRecord;
import com.glodon.mordor.yansen.config.store.ToolConfigRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteConfigStoreDeleteReferenceTest {

    @TempDir
    Path tempDir;

    private SqliteConfigStore store;

    @BeforeEach
    void setUp() {
        store = new SqliteConfigStore(tempDir.resolve("test.db").toString());
    }

    @AfterEach
    void tearDown() {
        store.close();
    }

    @Test
    void deleteModel_whenReferencedByAgent_throwsAndKeepsRecord() {
        ConfigReferenceException ex = assertThrows(
                ConfigReferenceException.class, () -> store.deleteModel("default"));

        assertEquals("model", ex.entityType());
        assertEquals("default", ex.entityId());
        assertEquals(List.of("default"), ex.referencingAgents());
        assertTrue(store.getModel("default").isPresent());
    }

    @Test
    void deletePrompt_whenReferencedByAgent_throwsAndKeepsRecord() {
        ConfigReferenceException ex = assertThrows(
                ConfigReferenceException.class, () -> store.deletePrompt(1));

        assertEquals("prompt", ex.entityType());
        assertEquals("1", ex.entityId());
        assertEquals(List.of("default"), ex.referencingAgents());
        assertTrue(store.getPrompt(1).isPresent());
    }

    @Test
    void deleteTool_whenReferencedByAgent_throwsAndKeepsRecord() {
        ConfigReferenceException ex = assertThrows(
                ConfigReferenceException.class, () -> store.deleteTool("datetime"));

        assertEquals("tool", ex.entityType());
        assertEquals("datetime", ex.entityId());
        assertEquals(List.of("default"), ex.referencingAgents());
        assertTrue(store.getTool("datetime").isPresent());
    }

    @Test
    void deleteSkill_whenReferencedByAgent_throwsAndKeepsRecord() {
        ConfigReferenceException ex = assertThrows(
                ConfigReferenceException.class, () -> store.deleteSkill("default"));

        assertEquals("skill", ex.entityType());
        assertEquals("default", ex.entityId());
        assertEquals(List.of("default"), ex.referencingAgents());
        assertTrue(store.getSkill("default").isPresent());
    }

    @Test
    void deleteMcp_whenReferencedByAgent_throwsAndKeepsRecord() {
        store.createMcp(new McpConfigRecord("test-mcp", "Test MCP", "{}", null, null));
        store.setAgentMcp("default", List.of("test-mcp"));

        ConfigReferenceException ex = assertThrows(
                ConfigReferenceException.class, () -> store.deleteMcp("test-mcp"));

        assertEquals("mcp", ex.entityType());
        assertEquals("test-mcp", ex.entityId());
        assertEquals(List.of("default"), ex.referencingAgents());
        assertTrue(store.getMcp("test-mcp").isPresent());
    }

    @Test
    void deleteModel_whenNotReferenced_succeeds() {
        store.createModel(orphanModel("orphan-model"));
        store.deleteModel("orphan-model");
        assertTrue(store.getModel("orphan-model").isEmpty());
    }

    @Test
    void deletePrompt_whenNotReferenced_succeeds() {
        SystemPromptRecord created = store.createPrompt(
                new SystemPromptRecord(null, "orphan-prompt", "inline", "hello", null, null));
        store.deletePrompt(created.id());
        assertTrue(store.getPrompt(created.id()).isEmpty());
    }

    @Test
    void deleteTool_whenNotReferenced_succeeds() {
        store.createTool(new ToolConfigRecord("orphan-tool", "Orphan", "desc", true, null, null));
        store.deleteTool("orphan-tool");
        assertTrue(store.getTool("orphan-tool").isEmpty());
    }

    @Test
    void deleteSkill_whenNotReferenced_succeeds() {
        store.createSkill(new SkillConfigRecord(
                "orphan-skill", "Orphan", "classpath", "skills", null, null));
        store.deleteSkill("orphan-skill");
        assertTrue(store.getSkill("orphan-skill").isEmpty());
    }

    @Test
    void deleteMcp_whenNotReferenced_succeeds() {
        store.createMcp(new McpConfigRecord("orphan-mcp", "Orphan MCP", "{}", null, null));
        store.deleteMcp("orphan-mcp");
        assertTrue(store.getMcp("orphan-mcp").isEmpty());
    }

    private static ModelConfigRecord orphanModel(String modelId) {
        return new ModelConfigRecord(
                modelId, "openai-compatible", "Test", "https://example.com/v1", "key",
                3, 30, 900, 30, null, null);
    }
}
