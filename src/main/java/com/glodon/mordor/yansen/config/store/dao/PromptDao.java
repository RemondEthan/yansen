package com.glodon.mordor.yansen.config.store.dao;

import com.glodon.mordor.yansen.config.store.SystemPromptRecord;

import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Data access interface for system_prompt table.
 */
public interface PromptDao {
    
    String TABLE = "system_prompt";
    
    interface Field {
        String ID = "id";
        String NAME = "name";
        String SOURCE_TYPE = "sourceType";
        String SOURCE_REF = "sourceRef";
        String CREATED_AT = "createdAt";
        String UPDATED_AT = "updatedAt";
    }
    
    String SELECT_ALL = """
        SELECT {fields} FROM {table}
        """.replace("{fields}", String.join(", ", Field.ID, Field.NAME, Field.SOURCE_TYPE, Field.SOURCE_REF, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{table}", TABLE);
    
    String SELECT_BY_ID = SELECT_ALL + " WHERE " + Field.ID + " = ?";
    
    String INSERT = """
        INSERT INTO {table} ({fields}) VALUES ({placeholders})
        """.replace("{table}", TABLE)
        .replace("{fields}", String.join(", ", Field.NAME, Field.SOURCE_TYPE, Field.SOURCE_REF, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{placeholders}", "?, ?, ?, ?, ?");
    
    String UPDATE = """
        UPDATE {table} SET {set} WHERE {pk}=?
        """.replace("{table}", TABLE)
        .replace("{set}", String.join(", ", Field.NAME + "=?", Field.SOURCE_TYPE + "=?", Field.SOURCE_REF + "=?", Field.UPDATED_AT + "=?"))
        .replace("{pk}", Field.ID);
    
    String DELETE = "DELETE FROM " + TABLE + " WHERE " + Field.ID + " = ?";
    
    List<SystemPromptRecord> list();
    Optional<SystemPromptRecord> get(int id);
    SystemPromptRecord create(SystemPromptRecord record);
    SystemPromptRecord update(SystemPromptRecord record);
    void delete(int id);
}
