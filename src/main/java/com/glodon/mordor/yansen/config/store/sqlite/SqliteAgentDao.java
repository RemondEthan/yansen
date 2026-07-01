package com.glodon.mordor.yansen.config.store.sqlite;
import com.glodon.mordor.yansen.config.store.AppDataSource;

import com.glodon.mordor.yansen.config.store.AgentConfigRecord;
import com.glodon.mordor.yansen.config.store.dao.AgentDao;
import com.glodon.mordor.yansen.config.store.dao.AgentDao.Field;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of AgentDao.
 */
public class SqliteAgentDao extends SqliteDao<AgentConfigRecord> implements AgentDao {
    
    public SqliteAgentDao(AppDataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    public List<AgentConfigRecord> list() {
        try (Connection conn = dataSource.getConnection()) {
            return query(conn, AgentDao.SELECT_ALL);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentDao.SELECT_ALL, e);
        }
    }
    
    @Override
    public Optional<AgentConfigRecord> get(String agentId) {
        try (Connection conn = dataSource.getConnection()) {
            List<AgentConfigRecord> results = query(conn, AgentDao.SELECT_BY_ID, agentId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentDao.SELECT_BY_ID, e);
        }
    }
    
    @Override
    public AgentConfigRecord create(AgentConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            create(conn, record);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + AgentDao.INSERT, e);
        }
        return new AgentConfigRecord(
            record.agentId(), record.name(), record.agentType(), record.route(),
            record.modelId(), record.systemPromptId(), record.workspace(),
            record.toolIds(), record.skillIds(), record.mcpIds(),
            now(), now());
    }

    void create(Connection conn, AgentConfigRecord record) {
        String now = now();
        execute(conn, AgentDao.INSERT,
            record.agentId(), record.name(), record.agentType(), record.route(),
            record.modelId(), record.systemPromptId(), record.workspace(), now, now);
    }
    
    @Override
    public AgentConfigRecord update(AgentConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            update(conn, record);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + AgentDao.UPDATE, e);
        }
        return new AgentConfigRecord(
            record.agentId(), record.name(), record.agentType(), record.route(),
            record.modelId(), record.systemPromptId(), record.workspace(),
            record.toolIds(), record.skillIds(), record.mcpIds(),
            record.createdAt(), now());
    }

    void update(Connection conn, AgentConfigRecord record) {
        String now = now();
        execute(conn, AgentDao.UPDATE,
            record.name(), record.agentType(), record.route(), record.modelId(),
            record.systemPromptId(), record.workspace(), now, record.agentId());
    }

    @Override
    public void delete(String agentId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            delete(conn, agentId);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + AgentDao.DELETE, e);
        }
    }

    void delete(Connection conn, String agentId) {
        execute(conn, AgentDao.DELETE, agentId);
    }
    
    @Override
    protected AgentConfigRecord parse(ResultSet rs) throws SQLException {
        return new AgentConfigRecord(
            rs.getString(Field.AGENT_ID),
            rs.getString(Field.NAME),
            rs.getString(Field.AGENT_TYPE),
            rs.getString(Field.ROUTE),
            rs.getString(Field.MODEL_ID),
            (Integer) rs.getObject(Field.SYSTEM_PROMPT_ID),
            rs.getString(Field.WORKSPACE),
            null, null, null,
            rs.getString(Field.CREATED_AT),
            rs.getString(Field.UPDATED_AT));
    }
}
