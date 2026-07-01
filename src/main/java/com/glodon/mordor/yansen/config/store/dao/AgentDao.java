package com.glodon.mordor.yansen.config.store.dao;

import com.glodon.mordor.yansen.config.store.AgentConfigRecord;

import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Data access interface for agent_config table.
 */
public interface AgentDao {
    
    String TABLE = "agent_config";
    
    interface Field {
        String AGENT_ID = "agentId";
        String NAME = "name";
        String AGENT_TYPE = "agentType";
        String ROUTE = "route";
        String MODEL_ID = "modelId";
        String SYSTEM_PROMPT_ID = "systemPromptId";
        String WORKSPACE = "workspace";
        String CREATED_AT = "createdAt";
        String UPDATED_AT = "updatedAt";
    }
    
    String SELECT_ALL = """
        SELECT {fields} FROM {table}
        """.replace("{fields}", String.join(", ",
            Field.AGENT_ID, Field.NAME, Field.AGENT_TYPE, Field.ROUTE, Field.MODEL_ID,
            Field.SYSTEM_PROMPT_ID, Field.WORKSPACE, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{table}", TABLE);
    
    String SELECT_BY_ID = SELECT_ALL + " WHERE " + Field.AGENT_ID + " = ?";
    
    String INSERT = """
        INSERT INTO {table} ({fields}) VALUES ({placeholders})
        """.replace("{table}", TABLE)
        .replace("{fields}", String.join(", ",
            Field.AGENT_ID, Field.NAME, Field.AGENT_TYPE, Field.ROUTE, Field.MODEL_ID,
            Field.SYSTEM_PROMPT_ID, Field.WORKSPACE, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{placeholders}", "?, ?, ?, ?, ?, ?, ?, ?, ?");
    
    String UPDATE = """
        UPDATE {table} SET {set} WHERE {pk}=?
        """.replace("{table}", TABLE)
        .replace("{set}", String.join(", ",
            Field.NAME + "=?", Field.AGENT_TYPE + "=?", Field.ROUTE + "=?", Field.MODEL_ID + "=?",
            Field.SYSTEM_PROMPT_ID + "=?", Field.WORKSPACE + "=?", Field.UPDATED_AT + "=?"))
        .replace("{pk}", Field.AGENT_ID);
    
    String DELETE = "DELETE FROM " + TABLE + " WHERE " + Field.AGENT_ID + " = ?";
    
    List<AgentConfigRecord> list();
    Optional<AgentConfigRecord> get(String agentId);
    AgentConfigRecord create(AgentConfigRecord record);
    AgentConfigRecord update(AgentConfigRecord record);
    void delete(String agentId);
}
