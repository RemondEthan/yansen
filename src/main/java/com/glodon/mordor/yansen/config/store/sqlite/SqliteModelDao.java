package com.glodon.mordor.yansen.config.store.sqlite;
import com.glodon.mordor.yansen.config.store.AppDataSource;

import com.glodon.mordor.yansen.config.store.ModelConfigRecord;
import com.glodon.mordor.yansen.config.store.dao.ModelDao;
import com.glodon.mordor.yansen.config.store.dao.ModelDao.Field;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of ModelDao.
 */
public class SqliteModelDao extends SqliteDao<ModelConfigRecord> implements ModelDao {
    
    public SqliteModelDao(AppDataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    public List<ModelConfigRecord> list() {
        try (Connection conn = dataSource.getConnection()) {
            return query(conn, ModelDao.SELECT_ALL);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + ModelDao.SELECT_ALL, e);
        }
    }
    
    @Override
    public Optional<ModelConfigRecord> get(String modelId) {
        try (Connection conn = dataSource.getConnection()) {
            List<ModelConfigRecord> results = query(conn, ModelDao.SELECT_BY_ID, modelId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + ModelDao.SELECT_BY_ID, e);
        }
    }
    
    @Override
    public ModelConfigRecord create(ModelConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, ModelDao.INSERT,
                record.modelId(), record.provider(), record.modelName(), record.baseUrl(), record.apiKey(),
                record.maxRetries(), record.connectTimeoutSeconds(), record.readTimeoutSeconds(), record.writeTimeoutSeconds(),
                now, now);
            conn.commit();
            return new ModelConfigRecord(
                record.modelId(), record.provider(), record.modelName(), record.baseUrl(), record.apiKey(),
                record.maxRetries(), record.connectTimeoutSeconds(), record.readTimeoutSeconds(), record.writeTimeoutSeconds(),
                now, now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + ModelDao.INSERT, e);
        }
    }
    
    @Override
    public ModelConfigRecord update(ModelConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, ModelDao.UPDATE,
                record.provider(), record.modelName(), record.baseUrl(), record.apiKey(),
                record.maxRetries(), record.connectTimeoutSeconds(), record.readTimeoutSeconds(), record.writeTimeoutSeconds(),
                now, record.modelId());
            conn.commit();
            return new ModelConfigRecord(
                record.modelId(), record.provider(), record.modelName(), record.baseUrl(), record.apiKey(),
                record.maxRetries(), record.connectTimeoutSeconds(), record.readTimeoutSeconds(), record.writeTimeoutSeconds(),
                record.createdAt(), now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + ModelDao.UPDATE, e);
        }
    }
    
    @Override
    public void delete(String modelId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            execute(conn, ModelDao.DELETE, modelId);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + ModelDao.DELETE, e);
        }
    }
    
    @Override
    protected ModelConfigRecord parse(ResultSet rs) throws SQLException {
        return new ModelConfigRecord(
            rs.getString(Field.MODEL_ID),
            rs.getString(Field.PROVIDER),
            rs.getString(Field.MODEL_NAME),
            rs.getString(Field.BASE_URL),
            rs.getString(Field.API_KEY),
            (Integer) rs.getObject(Field.MAX_RETRIES),
            (Integer) rs.getObject(Field.CONNECT_TIMEOUT),
            (Integer) rs.getObject(Field.READ_TIMEOUT),
            (Integer) rs.getObject(Field.WRITE_TIMEOUT),
            rs.getString(Field.CREATED_AT),
            rs.getString(Field.UPDATED_AT));
    }
}
