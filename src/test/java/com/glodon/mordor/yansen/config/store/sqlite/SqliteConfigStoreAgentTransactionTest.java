package com.glodon.mordor.yansen.config.store.sqlite;

import com.glodon.mordor.yansen.config.store.AgentConfigRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteConfigStoreAgentTransactionTest {

    @TempDir
    Path tempDir;

    private SqliteConfigStore store;

    @BeforeEach
    void setUp() {
        store = new SqliteConfigStore(tempDir.resolve("agent-tx.db").toString());
    }

    @AfterEach
    void tearDown() {
        store.close();
    }

    @Test
    void createAgent_rollsBackWhenAssociationInsertFails() {
        int promptId = store.listPrompts().getFirst().id();
        AgentConfigRecord record = new AgentConfigRecord(
                "tx-rollback", "Tx test", "default", "/api/tx-rollback", "default",
                promptId, "./agentscope",
                List.of("datetime", "datetime"), List.of(), List.of(), null, null);

        assertThrows(RuntimeException.class, () -> store.createAgent(record));
        assertTrue(store.getAgent("tx-rollback").isEmpty());
    }

    @Test
    void updateAgent_rollsBackWhenAssociationInsertFails() {
        int promptId = store.listPrompts().getFirst().id();
        AgentConfigRecord created = store.createAgent(new AgentConfigRecord(
                "tx-update", "Tx update", "default", "/api/tx-update", "default",
                promptId, "./agentscope",
                List.of("datetime"), List.of("default"), List.of(), null, null));

        AgentConfigRecord badUpdate = new AgentConfigRecord(
                created.agentId(), created.name(), created.agentType(), created.route(),
                created.modelId(), created.systemPromptId(), created.workspace(),
                List.of("datetime", "datetime"), created.skillIds(), created.mcpIds(),
                created.createdAt(), created.updatedAt());

        assertThrows(RuntimeException.class, () -> store.updateAgent(badUpdate));
        assertEquals(List.of("datetime"), store.getAgentTools("tx-update"));
    }
}
