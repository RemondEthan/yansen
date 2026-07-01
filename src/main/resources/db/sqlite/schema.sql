-- Yansen configuration database schema
-- All tables use CREATE TABLE IF NOT EXISTS for idempotent execution.

-- ============================================================
-- Model configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS model_config (
    modelId             TEXT NOT NULL PRIMARY KEY,
    provider            TEXT NOT NULL,
    modelName           TEXT NOT NULL,
    baseUrl             TEXT NOT NULL,
    apiKey              TEXT NOT NULL DEFAULT '',
    maxRetries          INTEGER,
    connectTimeoutSeconds INTEGER,
    readTimeoutSeconds    INTEGER,
    writeTimeoutSeconds   INTEGER,
    createdAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updatedAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- ============================================================
-- System prompt
-- ============================================================
CREATE TABLE IF NOT EXISTS system_prompt (
    id                  INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name                TEXT NOT NULL UNIQUE,
    sourceType          TEXT NOT NULL DEFAULT 'inline',
    sourceRef           TEXT NOT NULL DEFAULT '',
    createdAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updatedAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- ============================================================
-- Tool configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS tool_config (
    toolId              TEXT NOT NULL PRIMARY KEY,
    name                TEXT NOT NULL,
    description         TEXT NOT NULL DEFAULT '',
    enabled             INTEGER NOT NULL DEFAULT 1,
    createdAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updatedAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- ============================================================
-- Skill configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS skill_config (
    skillId             TEXT NOT NULL PRIMARY KEY,
    name                TEXT NOT NULL,
    sourceType          TEXT NOT NULL DEFAULT 'classpath',
    sourceRef           TEXT NOT NULL DEFAULT '',
    createdAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updatedAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- ============================================================
-- MCP configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS mcp_config (
    mcpId               TEXT NOT NULL PRIMARY KEY,
    name                TEXT NOT NULL,
    config              TEXT NOT NULL DEFAULT '{}',
    createdAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updatedAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- ============================================================
-- Agent configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_config (
    agentId             TEXT NOT NULL PRIMARY KEY,
    name                TEXT NOT NULL,
    agentType           TEXT NOT NULL DEFAULT 'default',
    route               TEXT NOT NULL,
    modelId             TEXT NOT NULL,
    systemPromptId      INTEGER,
    workspace           TEXT NOT NULL DEFAULT '',
    createdAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updatedAt           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    FOREIGN KEY (modelId)        REFERENCES model_config(modelId),
    FOREIGN KEY (systemPromptId) REFERENCES system_prompt(id)
);

-- ============================================================
-- Agent association tables (many-to-many)
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_tool (
    agentId             TEXT NOT NULL,
    toolId              TEXT NOT NULL,
    PRIMARY KEY (agentId, toolId),
    FOREIGN KEY (agentId) REFERENCES agent_config(agentId),
    FOREIGN KEY (toolId)  REFERENCES tool_config(toolId)
);

CREATE TABLE IF NOT EXISTS agent_skill (
    agentId             TEXT NOT NULL,
    skillId             TEXT NOT NULL,
    PRIMARY KEY (agentId, skillId),
    FOREIGN KEY (agentId) REFERENCES agent_config(agentId),
    FOREIGN KEY (skillId) REFERENCES skill_config(skillId)
);

CREATE TABLE IF NOT EXISTS agent_mcp (
    agentId             TEXT NOT NULL,
    mcpId               TEXT NOT NULL,
    PRIMARY KEY (agentId, mcpId),
    FOREIGN KEY (agentId) REFERENCES agent_config(agentId),
    FOREIGN KEY (mcpId)  REFERENCES mcp_config(mcpId)
);
