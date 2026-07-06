package com.glodon.mordor.yansen.config.store.sqlite;

import com.glodon.mordor.yansen.config.store.dao.AgentAssociationDao;
import com.glodon.mordor.yansen.config.store.AppDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of AgentAssociationDao.
 * This DAO returns String (agentId) rather than a record object,
 * so it does not extend SqliteDao directly and provides its own queryList().
 */
public class SqliteAgentAssociationDao implements AgentAssociationDao {
    
    private final AppDataSource dataSource;
    
    public SqliteAgentAssociationDao(AppDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public List<String> getToolIds(String agentId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.SELECT_TOOL_IDS, agentId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.SELECT_TOOL_IDS, e);
        }
    }
    
    @Override
    public void setToolIds(String agentId, List<String> toolIds) {
        SqliteTransactions.inTransaction(dataSource, "set agent tools", conn -> setToolIds(conn, agentId, toolIds));
    }

    /** Replaces tool associations on a caller-managed connection; does not commit. */
    void setToolIds(Connection conn, String agentId, List<String> toolIds) {
        execute(conn, AgentAssociationDao.DELETE_TOOLS, agentId);
        for (String toolId : toolIds) {
            execute(conn, AgentAssociationDao.INSERT_TOOL, agentId, toolId);
        }
    }
    
    @Override
    public List<String> getSkillIds(String agentId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.SELECT_SKILL_IDS, agentId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.SELECT_SKILL_IDS, e);
        }
    }
    
    @Override
    public void setSkillIds(String agentId, List<String> skillIds) {
        SqliteTransactions.inTransaction(dataSource, "set agent skills", conn -> setSkillIds(conn, agentId, skillIds));
    }

    /** Replaces skill associations on a caller-managed connection; does not commit. */
    void setSkillIds(Connection conn, String agentId, List<String> skillIds) {
        execute(conn, AgentAssociationDao.DELETE_SKILLS, agentId);
        for (String skillId : skillIds) {
            execute(conn, AgentAssociationDao.INSERT_SKILL, agentId, skillId);
        }
    }
    
    @Override
    public List<String> getMcpIds(String agentId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.SELECT_MCP_IDS, agentId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.SELECT_MCP_IDS, e);
        }
    }
    
    @Override
    public void setMcpIds(String agentId, List<String> mcpIds) {
        SqliteTransactions.inTransaction(dataSource, "set agent mcps", conn -> setMcpIds(conn, agentId, mcpIds));
    }

    /** Replaces MCP associations on a caller-managed connection; does not commit. */
    void setMcpIds(Connection conn, String agentId, List<String> mcpIds) {
        execute(conn, AgentAssociationDao.DELETE_MCPS, agentId);
        for (String mcpId : mcpIds) {
            execute(conn, AgentAssociationDao.INSERT_MCP, agentId, mcpId);
        }
    }
    
    @Override
    public List<String> findAgentsByModel(String modelId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.FIND_BY_MODEL, modelId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.FIND_BY_MODEL, e);
        }
    }
    
    @Override
    public List<String> findAgentsByPrompt(int promptId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.FIND_BY_PROMPT, promptId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.FIND_BY_PROMPT, e);
        }
    }
    
    @Override
    public List<String> findAgentsByTool(String toolId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.FIND_BY_TOOL, toolId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.FIND_BY_TOOL, e);
        }
    }
    
    @Override
    public List<String> findAgentsBySkill(String skillId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.FIND_BY_SKILL, skillId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.FIND_BY_SKILL, e);
        }
    }
    
    @Override
    public List<String> findAgentsByMcp(String mcpId) {
        try (Connection conn = dataSource.getConnection()) {
            return queryList(conn, AgentAssociationDao.FIND_BY_MCP, mcpId);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + AgentAssociationDao.FIND_BY_MCP, e);
        }
    }
    
    private List<String> queryList(Connection conn, String sql, Object... params) {
        try (PreparedStatement stmt = prepare(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            ArrayList<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }
    
    private void execute(Connection conn, String sql, Object... params) {
        try (PreparedStatement stmt = prepare(conn, sql, params)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + sql, e);
        }
    }
    
    private PreparedStatement prepare(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt;
    }
}
