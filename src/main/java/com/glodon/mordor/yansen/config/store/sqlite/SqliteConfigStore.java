package com.glodon.mordor.yansen.config.store.sqlite;

import com.glodon.mordor.yansen.config.PlaceholderResolver;
import com.glodon.mordor.yansen.config.store.AgentConfigRecord;
import com.glodon.mordor.yansen.config.store.ConfigReferenceException;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import com.glodon.mordor.yansen.config.store.McpConfigRecord;
import com.glodon.mordor.yansen.config.store.ModelConfigRecord;
import com.glodon.mordor.yansen.config.store.dao.McpDao;
import com.glodon.mordor.yansen.config.store.dao.ModelDao;
import com.glodon.mordor.yansen.config.store.dao.PromptDao;
import com.glodon.mordor.yansen.config.store.SkillConfigRecord;
import com.glodon.mordor.yansen.config.store.SystemPromptRecord;
import com.glodon.mordor.yansen.config.store.ToolConfigRecord;
import com.glodon.mordor.yansen.config.store.dao.SkillDao;
import com.glodon.mordor.yansen.config.store.dao.ToolDao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of ConfigStore.
 * Uses HikariCP connection pool for thread-safe concurrent access.
 * Transaction lifecycle is managed by each DAO method independently (try-with-resources pattern).
 * This store class is a thin delegation layer — no transaction management needed here.
 */
public class SqliteConfigStore implements ConfigStore {
    
    private final SqliteDataSource dataSource;
    private final ModelDao modelDao;
    private final PromptDao promptDao;
    private final ToolDao toolDao;
    private final SkillDao skillDao;
    private final McpDao mcpDao;
    private final SqliteAgentDao agentDao;
    private final SqliteAgentAssociationDao associationDao;
    
    public SqliteConfigStore(String dbPath) {
        this.dataSource = new SqliteDataSource(dbPath);
        
        this.modelDao = new SqliteModelDao(dataSource);
        this.promptDao = new SqlitePromptDao(dataSource);
        this.toolDao = new SqliteToolDao(dataSource);
        this.skillDao = new SqliteSkillDao(dataSource);
        this.mcpDao = new SqliteMcpDao(dataSource);
        this.associationDao = new SqliteAgentAssociationDao(dataSource);
        this.agentDao = new SqliteAgentDao(dataSource);
        
        initSchema();
    }
    
    private static final String SCHEMA_RESOURCE = "db/sqlite/schema.sql";
    private static final String INIT_DATA_RESOURCE = "db/init-data.sql";

    private void initSchema() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                executeResource(conn, SCHEMA_RESOURCE);
                if (isEmpty(conn, "agent_config")) {
                    executeResourceWithPlaceholder(conn, INIT_DATA_RESOURCE);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw new RuntimeException("Failed to initialize schema", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema", e);
        }
    }
    
    private boolean isEmpty(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }
    
    private void executeResource(Connection conn, String resourcePath) throws SQLException {
        String sql = readResource(resourcePath);
        executeMultiStatement(conn, sql);
    }
    
    private void executeResourceWithPlaceholder(Connection conn, String resourcePath) throws SQLException {
        String sql = readResource(resourcePath);
        StringBuilder resolved = new StringBuilder();
        for (String line : sql.split("\n")) {
            resolved.append(PlaceholderResolver.resolve(line)).append("\n");
        }
        executeMultiStatement(conn, resolved.toString());
    }
    
    private void executeMultiStatement(Connection conn, String sql) throws SQLException {
        SqlScriptExecutor.execute(conn, sql);
    }
    
    private String readResource(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }
    
    // ========== Model ==========
    
    @Override
    public List<ModelConfigRecord> listModels() {
        return modelDao.list();
    }
    
    @Override
    public Optional<ModelConfigRecord> getModel(String modelId) {
        return modelDao.get(modelId);
    }
    
    @Override
    public ModelConfigRecord createModel(ModelConfigRecord record) {
        return modelDao.create(record);
    }
    
    @Override
    public ModelConfigRecord updateModel(ModelConfigRecord record) {
        return modelDao.update(record);
    }
    
    @Override
    public void deleteModel(String modelId) {
        ensureNotReferenced("model", modelId, findAgentsReferencingModel(modelId));
        modelDao.delete(modelId);
    }
    
    // ========== System Prompt ==========
    
    @Override
    public List<SystemPromptRecord> listPrompts() {
        return promptDao.list();
    }
    
    @Override
    public Optional<SystemPromptRecord> getPrompt(int id) {
        return promptDao.get(id);
    }
    
    @Override
    public SystemPromptRecord createPrompt(SystemPromptRecord record) {
        return promptDao.create(record);
    }
    
    @Override
    public SystemPromptRecord updatePrompt(SystemPromptRecord record) {
        return promptDao.update(record);
    }
    
    @Override
    public void deletePrompt(int id) {
        ensureNotReferenced("prompt", String.valueOf(id), findAgentsReferencingPrompt(id));
        promptDao.delete(id);
    }
    
    @Override
    public String resolvePromptContent(SystemPromptRecord record) {
        return switch (record.sourceType()) {
            case "inline" -> record.sourceRef();
            case "file" -> {
                try {
                    yield Files.readString(Path.of(record.sourceRef()));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read prompt file: " + record.sourceRef(), e);
                }
            }
            case "classpath" -> readResource(record.sourceRef());
            default -> throw new IllegalArgumentException("Unknown source type: " + record.sourceType());
        };
    }
    
    // ========== Tool ==========
    
    @Override
    public List<ToolConfigRecord> listTools() {
        return toolDao.list();
    }
    
    @Override
    public Optional<ToolConfigRecord> getTool(String toolId) {
        return toolDao.get(toolId);
    }
    
    @Override
    public ToolConfigRecord createTool(ToolConfigRecord record) {
        return toolDao.create(record);
    }
    
    @Override
    public ToolConfigRecord updateTool(ToolConfigRecord record) {
        return toolDao.update(record);
    }
    
    @Override
    public void deleteTool(String toolId) {
        ensureNotReferenced("tool", toolId, findAgentsReferencingTool(toolId));
        toolDao.delete(toolId);
    }
    
    // ========== Skill ==========
    
    @Override
    public List<SkillConfigRecord> listSkills() {
        return skillDao.list();
    }
    
    @Override
    public Optional<SkillConfigRecord> getSkill(String skillId) {
        return skillDao.get(skillId);
    }
    
    @Override
    public SkillConfigRecord createSkill(SkillConfigRecord record) {
        return skillDao.create(record);
    }
    
    @Override
    public SkillConfigRecord updateSkill(SkillConfigRecord record) {
        return skillDao.update(record);
    }
    
    @Override
    public void deleteSkill(String skillId) {
        ensureNotReferenced("skill", skillId, findAgentsReferencingSkill(skillId));
        skillDao.delete(skillId);
    }
    
    // ========== MCP ==========
    
    @Override
    public List<McpConfigRecord> listMcp() {
        return mcpDao.list();
    }
    
    @Override
    public Optional<McpConfigRecord> getMcp(String mcpId) {
        return mcpDao.get(mcpId);
    }
    
    @Override
    public McpConfigRecord createMcp(McpConfigRecord record) {
        return mcpDao.create(record);
    }
    
    @Override
    public McpConfigRecord updateMcp(McpConfigRecord record) {
        return mcpDao.update(record);
    }
    
    @Override
    public void deleteMcp(String mcpId) {
        ensureNotReferenced("mcp", mcpId, findAgentsReferencingMcp(mcpId));
        mcpDao.delete(mcpId);
    }
    
    // ========== Agent ==========
    
    @Override
    public List<AgentConfigRecord> listAgents() {
        return agentDao.list().stream().map(this::enrichAgent).toList();
    }
    
    @Override
    public Optional<AgentConfigRecord> getAgent(String agentId) {
        return agentDao.get(agentId).map(this::enrichAgent);
    }
    
    @Override
    public AgentConfigRecord createAgent(AgentConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                agentDao.create(conn, record);
                associationDao.setToolIds(conn, record.agentId(), record.toolIds());
                associationDao.setSkillIds(conn, record.agentId(), record.skillIds());
                associationDao.setMcpIds(conn, record.agentId(), record.mcpIds());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to create agent", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create agent", e);
        }
        return getAgent(record.agentId()).orElseThrow();
    }
    
    @Override
    public AgentConfigRecord updateAgent(AgentConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                agentDao.update(conn, record);
                associationDao.setToolIds(conn, record.agentId(), record.toolIds());
                associationDao.setSkillIds(conn, record.agentId(), record.skillIds());
                associationDao.setMcpIds(conn, record.agentId(), record.mcpIds());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to update agent", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update agent", e);
        }
        return getAgent(record.agentId()).orElseThrow();
    }
    
    @Override
    public void deleteAgent(String agentId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                associationDao.setToolIds(conn, agentId, List.of());
                associationDao.setSkillIds(conn, agentId, List.of());
                associationDao.setMcpIds(conn, agentId, List.of());
                agentDao.delete(conn, agentId);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to delete agent", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete agent", e);
        }
    }
    
    // ========== Agent associations ==========
    
    @Override
    public List<String> getAgentTools(String agentId) {
        return associationDao.getToolIds(agentId);
    }
    
    @Override
    public List<String> getAgentSkills(String agentId) {
        return associationDao.getSkillIds(agentId);
    }
    
    @Override
    public List<String> getAgentMcp(String agentId) {
        return associationDao.getMcpIds(agentId);
    }
    
    @Override
    public void setAgentTools(String agentId, List<String> toolIds) {
        associationDao.setToolIds(agentId, toolIds);
    }
    
    @Override
    public void setAgentSkills(String agentId, List<String> skillIds) {
        associationDao.setSkillIds(agentId, skillIds);
    }
    
    @Override
    public void setAgentMcp(String agentId, List<String> mcpIds) {
        associationDao.setMcpIds(agentId, mcpIds);
    }
    
    // ========== Reference checks ==========
    
    @Override
    public List<String> findAgentsReferencingModel(String modelId) {
        return associationDao.findAgentsByModel(modelId);
    }
    
    @Override
    public List<String> findAgentsReferencingPrompt(int promptId) {
        return associationDao.findAgentsByPrompt(promptId);
    }
    
    @Override
    public List<String> findAgentsReferencingTool(String toolId) {
        return associationDao.findAgentsByTool(toolId);
    }
    
    @Override
    public List<String> findAgentsReferencingSkill(String skillId) {
        return associationDao.findAgentsBySkill(skillId);
    }
    
    @Override
    public List<String> findAgentsReferencingMcp(String mcpId) {
        return associationDao.findAgentsByMcp(mcpId);
    }
    
    // ========== Helper methods ==========
    
    private AgentConfigRecord enrichAgent(AgentConfigRecord agent) {
        return new AgentConfigRecord(
            agent.agentId(), agent.name(), agent.agentType(), agent.route(),
            agent.modelId(), agent.systemPromptId(), agent.workspace(),
            associationDao.getToolIds(agent.agentId()),
            associationDao.getSkillIds(agent.agentId()),
            associationDao.getMcpIds(agent.agentId()),
            agent.createdAt(), agent.updatedAt());
    }

    private static void ensureNotReferenced(String entityType, String entityId,
                                            List<String> referencingAgents) {
        if (!referencingAgents.isEmpty()) {
            throw new ConfigReferenceException(entityType, entityId, referencingAgents);
        }
    }
    
    // ========== Lifecycle ==========
    
    @Override
    public void close() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        } catch (SQLException ignored) {}
        dataSource.close();
    }
}
