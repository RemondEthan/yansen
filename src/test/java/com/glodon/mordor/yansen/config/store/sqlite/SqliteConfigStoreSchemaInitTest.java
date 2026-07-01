package com.glodon.mordor.yansen.config.store.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteConfigStoreSchemaInitTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "model_config",
            "system_prompt",
            "tool_config",
            "skill_config",
            "mcp_config",
            "agent_config",
            "agent_tool",
            "agent_skill",
            "agent_mcp");

    @TempDir
    Path tempDir;

    private SqliteConfigStore store;

    @BeforeEach
    void setUp() {
        store = new SqliteConfigStore(tempDir.resolve("schema-init.db").toString());
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    @Test
    void initSchema_createsAllTablesAndSeedData() throws SQLException {
        for (String table : EXPECTED_TABLES) {
            assertTrue(tableExists(table), "missing table: " + table);
        }

        assertFalse(store.listModels().isEmpty());
        assertFalse(store.listAgents().isEmpty());
        assertTrue(store.getAgent("default").isPresent());
        assertTrue(store.getModel("default").isPresent());
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return new SqliteDataSource(tempDir.resolve("schema-init.db").toString()).getConnection();
    }
}
