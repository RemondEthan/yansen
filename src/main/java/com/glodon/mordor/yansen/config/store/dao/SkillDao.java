package com.glodon.mordor.yansen.config.store.dao;

import com.glodon.mordor.yansen.config.store.SkillConfigRecord;

import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Data access interface for skill_config table.
 */
public interface SkillDao {
    
    String TABLE = "skill_config";
    
    interface Field {
        String SKILL_ID = "skillId";
        String NAME = "name";
        String SOURCE_TYPE = "sourceType";
        String SOURCE_REF = "sourceRef";
        String CREATED_AT = "createdAt";
        String UPDATED_AT = "updatedAt";
    }
    
    String SELECT_ALL = """
        SELECT {fields} FROM {table}
        """.replace("{fields}", String.join(", ", Field.SKILL_ID, Field.NAME, Field.SOURCE_TYPE, Field.SOURCE_REF, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{table}", TABLE);
    
    String SELECT_BY_ID = SELECT_ALL + " WHERE " + Field.SKILL_ID + " = ?";
    
    String INSERT = """
        INSERT INTO {table} ({fields}) VALUES ({placeholders})
        """.replace("{table}", TABLE)
        .replace("{fields}", String.join(", ", Field.SKILL_ID, Field.NAME, Field.SOURCE_TYPE, Field.SOURCE_REF, Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{placeholders}", "?, ?, ?, ?, ?, ?");
    
    String UPDATE = """
        UPDATE {table} SET {set} WHERE {pk}=?
        """.replace("{table}", TABLE)
        .replace("{set}", String.join(", ", Field.NAME + "=?", Field.SOURCE_TYPE + "=?", Field.SOURCE_REF + "=?", Field.UPDATED_AT + "=?"))
        .replace("{pk}", Field.SKILL_ID);
    
    String DELETE = "DELETE FROM " + TABLE + " WHERE " + Field.SKILL_ID + " = ?";
    
    List<SkillConfigRecord> list();
    Optional<SkillConfigRecord> get(String skillId);
    SkillConfigRecord create(SkillConfigRecord record);
    SkillConfigRecord update(SkillConfigRecord record);
    void delete(String skillId);
}
