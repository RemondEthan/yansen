package com.glodon.mordor.yansen.config.store.sqlite;
import com.glodon.mordor.yansen.config.store.AppDataSource;

import com.glodon.mordor.yansen.config.store.ToolConfigRecord;
import com.glodon.mordor.yansen.config.store.dao.ToolDao;
import com.glodon.mordor.yansen.config.store.dao.ToolDao.Field;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of ToolDao.
 */
public class SqliteToolDao extends SqliteDao<ToolConfigRecord> implements ToolDao {
    
    public SqliteToolDao(AppDataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    public List<ToolConfigRecord> list() {
        try (Connection conn = dataSource.getConnection()) {
            return query(conn, ToolDao.SELECT_ALL);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + ToolDao.SELECT_ALL, e);
        }
    }
    
    @Override
    public Optional<ToolConfigRecord> get(String toolId) {
        try (Connection conn = dataSource.getConnection()) {
            List<ToolConfigRecord> results = query(conn, ToolDao.SELECT_BY_ID, toolId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + ToolDao.SELECT_BY_ID, e);
        }
    }
    
    @Override
    public ToolConfigRecord create(ToolConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, ToolDao.INSERT,
                record.toolId(), record.name(), record.description(), record.enabled() ? 1 : 0, now, now);
            conn.commit();
            return new ToolConfigRecord(record.toolId(), record.name(), record.description(), record.enabled(), now, now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + ToolDao.INSERT, e);
        }
    }
    
    @Override
    public ToolConfigRecord update(ToolConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, ToolDao.UPDATE,
                record.name(), record.description(), record.enabled() ? 1 : 0, now, record.toolId());
            conn.commit();
            return new ToolConfigRecord(record.toolId(), record.name(), record.description(), record.enabled(),
                record.createdAt(), now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + ToolDao.UPDATE, e);
        }
    }
    
    @Override
    public void delete(String toolId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            execute(conn, ToolDao.DELETE, toolId);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + ToolDao.DELETE, e);
        }
    }
    
    @Override
    protected ToolConfigRecord parse(ResultSet rs) throws SQLException {
        return new ToolConfigRecord(
            rs.getString(Field.TOOL_ID),
            rs.getString(Field.NAME),
            rs.getString(Field.DESCRIPTION),
            rs.getInt(Field.ENABLED) == 1,
            rs.getString(Field.CREATED_AT),
            rs.getString(Field.UPDATED_AT));
    }
}
