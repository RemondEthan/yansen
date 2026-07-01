package com.glodon.mordor.yansen.config.store.dao;

import java.util.List;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Data access interface for agent association tables
 * (agent_tool, agent_skill, agent_mcp).
 */
public interface AgentAssociationDao {
    
    String T_AGENT_TOOL = "agent_tool";
    String T_AGENT_SKILL = "agent_skill";
    String T_AGENT_MCP = "agent_mcp";
    String T_AGENT = "agent_config";
    
    interface Field {
        String AGENT_ID = "agentId";
        String TOOL_ID = "toolId";
        String SKILL_ID = "skillId";
        String MCP_ID = "mcpId";
        String MODEL_ID = "modelId";
        String SYSTEM_PROMPT_ID = "systemPromptId";
    }
    
    // Tool associations
    String SELECT_TOOL_IDS = "SELECT " + Field.TOOL_ID + " FROM " + T_AGENT_TOOL + " WHERE " + Field.AGENT_ID + " = ?";
    String INSERT_TOOL = "INSERT INTO " + T_AGENT_TOOL + " (" + Field.AGENT_ID + ", " + Field.TOOL_ID + ") VALUES (?, ?)";
    String DELETE_TOOLS = "DELETE FROM " + T_AGENT_TOOL + " WHERE " + Field.AGENT_ID + " = ?";
    
    // Skill associations
    String SELECT_SKILL_IDS = "SELECT " + Field.SKILL_ID + " FROM " + T_AGENT_SKILL + " WHERE " + Field.AGENT_ID + " = ?";
    String INSERT_SKILL = "INSERT INTO " + T_AGENT_SKILL + " (" + Field.AGENT_ID + ", " + Field.SKILL_ID + ") VALUES (?, ?)";
    String DELETE_SKILLS = "DELETE FROM " + T_AGENT_SKILL + " WHERE " + Field.AGENT_ID + " = ?";
    
    // MCP associations
    String SELECT_MCP_IDS = "SELECT " + Field.MCP_ID + " FROM " + T_AGENT_MCP + " WHERE " + Field.AGENT_ID + " = ?";
    String INSERT_MCP = "INSERT INTO " + T_AGENT_MCP + " (" + Field.AGENT_ID + ", " + Field.MCP_ID + ") VALUES (?, ?)";
    String DELETE_MCPS = "DELETE FROM " + T_AGENT_MCP + " WHERE " + Field.AGENT_ID + " = ?";
    
    // Reference checks
    String FIND_BY_MODEL = "SELECT " + Field.AGENT_ID + " FROM " + T_AGENT + " WHERE " + Field.MODEL_ID + " = ?";
    String FIND_BY_PROMPT = "SELECT " + Field.AGENT_ID + " FROM " + T_AGENT + " WHERE " + Field.SYSTEM_PROMPT_ID + " = ?";
    String FIND_BY_TOOL = "SELECT " + Field.AGENT_ID + " FROM " + T_AGENT_TOOL + " WHERE " + Field.TOOL_ID + " = ?";
    String FIND_BY_SKILL = "SELECT " + Field.AGENT_ID + " FROM " + T_AGENT_SKILL + " WHERE " + Field.SKILL_ID + " = ?";
    String FIND_BY_MCP = "SELECT " + Field.AGENT_ID + " FROM " + T_AGENT_MCP + " WHERE " + Field.MCP_ID + " = ?";
    
    // Methods
    List<String> getToolIds(String agentId);
    void setToolIds(String agentId, List<String> toolIds);
    
    List<String> getSkillIds(String agentId);
    void setSkillIds(String agentId, List<String> skillIds);
    
    List<String> getMcpIds(String agentId);
    void setMcpIds(String agentId, List<String> mcpIds);
    
    List<String> findAgentsByModel(String modelId);
    List<String> findAgentsByPrompt(int promptId);
    List<String> findAgentsByTool(String toolId);
    List<String> findAgentsBySkill(String skillId);
    List<String> findAgentsByMcp(String mcpId);
}
