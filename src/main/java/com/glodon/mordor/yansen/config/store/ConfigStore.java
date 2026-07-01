package com.glodon.mordor.yansen.config.store;

import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Read/write interface for all business configuration stored in SQLite.
 * Implementation handles JDBC lifecycle, schema initialization, and placeholder pre-rendering.
 *
 * <p><b>Timestamp convention:</b> All record types include {@code createdAt}/{@code updatedAt} fields.
 * On {@code create} and {@code update} calls, these fields in the input record are <em>ignored</em> —
 * the database generates them via DEFAULT/trigger. The returned record contains the actual DB-generated values.
 * Callers should pass {@code null} for these fields in input records.</p>
 */
public interface ConfigStore extends AutoCloseable {

    // ---- Model ----
    List<ModelConfigRecord> listModels();
    Optional<ModelConfigRecord> getModel(String modelId);
    ModelConfigRecord createModel(ModelConfigRecord record);
    ModelConfigRecord updateModel(ModelConfigRecord record);
    void deleteModel(String modelId);

    // ---- System Prompt ----
    List<SystemPromptRecord> listPrompts();
    Optional<SystemPromptRecord> getPrompt(int id);
    SystemPromptRecord createPrompt(SystemPromptRecord record);
    SystemPromptRecord updatePrompt(SystemPromptRecord record);
    void deletePrompt(int id);

    /**
     * Resolve prompt content based on sourceType: inline→sourceRef, file→read file, classpath→read resource.
     */
    String resolvePromptContent(SystemPromptRecord record);

    // ---- Tool ----
    List<ToolConfigRecord> listTools();
    Optional<ToolConfigRecord> getTool(String toolId);
    ToolConfigRecord createTool(ToolConfigRecord record);
    ToolConfigRecord updateTool(ToolConfigRecord record);
    void deleteTool(String toolId);

    // ---- Skill ----
    List<SkillConfigRecord> listSkills();
    Optional<SkillConfigRecord> getSkill(String skillId);
    SkillConfigRecord createSkill(SkillConfigRecord record);
    SkillConfigRecord updateSkill(SkillConfigRecord record);
    void deleteSkill(String skillId);

    // ---- MCP ----
    List<McpConfigRecord> listMcp();
    Optional<McpConfigRecord> getMcp(String mcpId);
    McpConfigRecord createMcp(McpConfigRecord record);
    McpConfigRecord updateMcp(McpConfigRecord record);
    void deleteMcp(String mcpId);

    // ---- Agent ----
    List<AgentConfigRecord> listAgents();
    Optional<AgentConfigRecord> getAgent(String agentId);
    AgentConfigRecord createAgent(AgentConfigRecord record);
    AgentConfigRecord updateAgent(AgentConfigRecord record);
    void deleteAgent(String agentId);

    // ---- Agent associations ----
    List<String> getAgentTools(String agentId);
    List<String> getAgentSkills(String agentId);
    List<String> getAgentMcp(String agentId);
    void setAgentTools(String agentId, List<String> toolIds);
    void setAgentSkills(String agentId, List<String> skillIds);
    void setAgentMcp(String agentId, List<String> mcpIds);

    // ---- Reference checks (for delete validation) ----
    List<String> findAgentsReferencingModel(String modelId);
    List<String> findAgentsReferencingPrompt(int promptId);
    List<String> findAgentsReferencingTool(String toolId);
    List<String> findAgentsReferencingSkill(String skillId);
    List<String> findAgentsReferencingMcp(String mcpId);
}
