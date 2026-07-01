package com.glodon.mordor.yansen.config.store.sqlite;
import com.glodon.mordor.yansen.config.store.AppDataSource;

import com.glodon.mordor.yansen.config.store.McpConfigRecord;
import com.glodon.mordor.yansen.config.store.dao.McpDao;
import com.glodon.mordor.yansen.config.store.dao.McpDao.Field;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of McpDao.
 */
public class SqliteMcpDao extends SqliteDao<McpConfigRecord> implements McpDao {
    
    public SqliteMcpDao(AppDataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    public List<McpConfigRecord> list() {
        try (Connection conn = dataSource.getConnection()) {
            return query(conn, McpDao.SELECT_ALL);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + McpDao.SELECT_ALL, e);
        }
    }
    
    @Override
    public Optional<McpConfigRecord> get(String mcpId) {
        try (Connection conn = dataSource.getConnection()) {
            List<McpConfigRecord> results = query(conn, McpDao.SELECT_BY_ID, mcpId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + McpDao.SELECT_BY_ID, e);
        }
    }
    
    @Override
    public McpConfigRecord create(McpConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, McpDao.INSERT,
                record.mcpId(), record.name(), record.config(), now, now);
            conn.commit();
            return new McpConfigRecord(record.mcpId(), record.name(), record.config(), now, now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + McpDao.INSERT, e);
        }
    }
    
    @Override
    public McpConfigRecord update(McpConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, McpDao.UPDATE,
                record.name(), record.config(), now, record.mcpId());
            conn.commit();
            return new McpConfigRecord(record.mcpId(), record.name(), record.config(), record.createdAt(), now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + McpDao.UPDATE, e);
        }
    }
    
    @Override
    public void delete(String mcpId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            execute(conn, McpDao.DELETE, mcpId);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + McpDao.DELETE, e);
        }
    }
    
    @Override
    protected McpConfigRecord parse(ResultSet rs) throws SQLException {
        return new McpConfigRecord(
            rs.getString(Field.MCP_ID),
            rs.getString(Field.NAME),
            rs.getString(Field.CONFIG),
            rs.getString(Field.CREATED_AT),
            rs.getString(Field.UPDATED_AT));
    }
}
