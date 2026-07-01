package com.glodon.mordor.yansen.config.store.sqlite;
import com.glodon.mordor.yansen.config.store.AppDataSource;

import com.glodon.mordor.yansen.config.store.SystemPromptRecord;
import com.glodon.mordor.yansen.config.store.dao.PromptDao;
import com.glodon.mordor.yansen.config.store.dao.PromptDao.Field;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of PromptDao.
 */
public class SqlitePromptDao extends SqliteDao<SystemPromptRecord> implements PromptDao {
    
    public SqlitePromptDao(AppDataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    public List<SystemPromptRecord> list() {
        try (Connection conn = dataSource.getConnection()) {
            return query(conn, PromptDao.SELECT_ALL);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + PromptDao.SELECT_ALL, e);
        }
    }
    
    @Override
    public Optional<SystemPromptRecord> get(int id) {
        try (Connection conn = dataSource.getConnection()) {
            List<SystemPromptRecord> results = query(conn, PromptDao.SELECT_BY_ID, id);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + PromptDao.SELECT_BY_ID, e);
        }
    }
    
    @Override
    public SystemPromptRecord create(SystemPromptRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            int generatedId = executeAndReturnKey(conn, PromptDao.INSERT,
                record.name(), record.sourceType(), record.sourceRef(), now, now);
            conn.commit();
            return new SystemPromptRecord(generatedId, record.name(), record.sourceType(), record.sourceRef(), now, now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + PromptDao.INSERT, e);
        }
    }
    
    @Override
    public SystemPromptRecord update(SystemPromptRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, PromptDao.UPDATE,
                record.name(), record.sourceType(), record.sourceRef(), now, record.id());
            conn.commit();
            return new SystemPromptRecord(record.id(), record.name(), record.sourceType(), record.sourceRef(),
                record.createdAt(), now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + PromptDao.UPDATE, e);
        }
    }
    
    @Override
    public void delete(int id) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            execute(conn, PromptDao.DELETE, id);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + PromptDao.DELETE, e);
        }
    }
    
    @Override
    protected SystemPromptRecord parse(ResultSet rs) throws SQLException {
        return new SystemPromptRecord(
            rs.getInt(Field.ID),
            rs.getString(Field.NAME),
            rs.getString(Field.SOURCE_TYPE),
            rs.getString(Field.SOURCE_REF),
            rs.getString(Field.CREATED_AT),
            rs.getString(Field.UPDATED_AT));
    }
}
