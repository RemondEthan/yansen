package com.glodon.mordor.yansen.config.store.dao;

import com.glodon.mordor.yansen.config.store.McpConfigRecord;

import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Data access interface for mcp_config table.
 */
public interface McpDao {
    
    String TABLE = "mcp_config";
    
    interface Field {
        String MCP_ID = "mcpId";
        String NAME = "name";
        String CONFIG = "config";
        String CREATED_AT = "createdAt";
        String UPDATED_AT = "updatedAt";
    }
    
    String SELECT_ALL = """
        SELECT {fields} FROM {table}
        """.replace("{fields}", String.join(", ", Field.MCP_ID, Field.NAME, Field.CONFIG, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{table}", TABLE);
    
    String SELECT_BY_ID = SELECT_ALL + " WHERE " + Field.MCP_ID + " = ?";
    
    String INSERT = """
        INSERT INTO {table} ({fields}) VALUES ({placeholders})
        """.replace("{table}", TABLE)
        .replace("{fields}", String.join(", ", Field.MCP_ID, Field.NAME, Field.CONFIG, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{placeholders}", "?, ?, ?, ?, ?");
    
    String UPDATE = """
        UPDATE {table} SET {set} WHERE {pk}=?
        """.replace("{table}", TABLE)
        .replace("{set}", String.join(", ", Field.NAME + "=?", Field.CONFIG + "=?", Field.UPDATED_AT + "=?"))
        .replace("{pk}", Field.MCP_ID);
    
    String DELETE = "DELETE FROM " + TABLE + " WHERE " + Field.MCP_ID + " = ?";
    
    List<McpConfigRecord> list();
    Optional<McpConfigRecord> get(String mcpId);
    McpConfigRecord create(McpConfigRecord record);
    McpConfigRecord update(McpConfigRecord record);
    void delete(String mcpId);
}
