package com.glodon.mordor.yansen.config.store.dao;

import com.glodon.mordor.yansen.config.store.ModelConfigRecord;

import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Data access interface for model_config table.
 */
public interface ModelDao {
    
    String TABLE = "model_config";
    
    interface Field {
        String MODEL_ID = "modelId";
        String PROVIDER = "provider";
        String MODEL_NAME = "modelName";
        String BASE_URL = "baseUrl";
        String API_KEY = "apiKey";
        String MAX_RETRIES = "maxRetries";
        String CONNECT_TIMEOUT = "connectTimeoutSeconds";
        String READ_TIMEOUT = "readTimeoutSeconds";
        String WRITE_TIMEOUT = "writeTimeoutSeconds";
        String CREATED_AT = "createdAt";
        String UPDATED_AT = "updatedAt";
    }
    
    String SELECT_ALL = """
        SELECT {fields} FROM {table}
        """.replace("{fields}", String.join(", ",
            Field.MODEL_ID, Field.PROVIDER, Field.MODEL_NAME, Field.BASE_URL, Field.API_KEY,
            Field.MAX_RETRIES, Field.CONNECT_TIMEOUT, Field.READ_TIMEOUT, Field.WRITE_TIMEOUT,
            Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{table}", TABLE);
    
    String SELECT_BY_ID = SELECT_ALL + " WHERE " + Field.MODEL_ID + " = ?";
    
    String INSERT = """
        INSERT INTO {table} ({fields}) VALUES ({placeholders})
        """.replace("{table}", TABLE)
        .replace("{fields}", String.join(", ",
            Field.MODEL_ID, Field.PROVIDER, Field.MODEL_NAME, Field.BASE_URL, Field.API_KEY,
            Field.MAX_RETRIES, Field.CONNECT_TIMEOUT, Field.READ_TIMEOUT, Field.WRITE_TIMEOUT,
            Field.CREATED_AT, Field.UPDATED_AT))
        .replace("{placeholders}", "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
    
    String UPDATE = """
        UPDATE {table} SET {set} WHERE {pk}=?
        """.replace("{table}", TABLE)
        .replace("{set}", String.join(", ",
            Field.PROVIDER + "=?", Field.MODEL_NAME + "=?", Field.BASE_URL + "=?", Field.API_KEY + "=?",
            Field.MAX_RETRIES + "=?", Field.CONNECT_TIMEOUT + "=?", Field.READ_TIMEOUT + "=?", Field.WRITE_TIMEOUT + "=?",
            Field.UPDATED_AT + "=?"))
        .replace("{pk}", Field.MODEL_ID);
    
    String DELETE = "DELETE FROM " + TABLE + " WHERE " + Field.MODEL_ID + " = ?";
    
    List<ModelConfigRecord> list();
    Optional<ModelConfigRecord> get(String modelId);
    ModelConfigRecord create(ModelConfigRecord record);
    ModelConfigRecord update(ModelConfigRecord record);
    void delete(String modelId);
}
