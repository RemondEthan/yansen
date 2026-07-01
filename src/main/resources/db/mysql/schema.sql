-- Yansen configuration database schema (MySQL)
-- All tables use CREATE TABLE IF NOT EXISTS for idempotent execution.
-- Cascade deletes are handled in application code, not via ON DELETE CASCADE.

-- ============================================================
-- Model configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS model_config (
    modelId               VARCHAR(255) NOT NULL,
    provider              VARCHAR(255) NOT NULL,
    modelName             VARCHAR(255) NOT NULL,
    baseUrl               VARCHAR(1024) NOT NULL,
    apiKey                VARCHAR(1024) NOT NULL DEFAULT '',
    maxRetries            INT,
    connectTimeoutSeconds INT,
    readTimeoutSeconds    INT,
    writeTimeoutSeconds   INT,
    createdAt             VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    updatedAt             VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    PRIMARY KEY (modelId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- System prompt
-- ============================================================
CREATE TABLE IF NOT EXISTS system_prompt (
    id          INT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    sourceType  VARCHAR(32) NOT NULL DEFAULT 'inline',
    sourceRef   TEXT NOT NULL,
    createdAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    updatedAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Tool configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS tool_config (
    toolId      VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(1024) NOT NULL DEFAULT '',
    enabled     TINYINT(1) NOT NULL DEFAULT 1,
    createdAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    updatedAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    PRIMARY KEY (toolId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Skill configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS skill_config (
    skillId     VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    sourceType  VARCHAR(32) NOT NULL DEFAULT 'classpath',
    sourceRef   VARCHAR(1024) NOT NULL DEFAULT '',
    createdAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    updatedAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    PRIMARY KEY (skillId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MCP configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS mcp_config (
    mcpId       VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    config      TEXT NOT NULL,
    createdAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    updatedAt   VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    PRIMARY KEY (mcpId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Agent configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_config (
    agentId         VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    agentType       VARCHAR(32) NOT NULL DEFAULT 'default',
    route           VARCHAR(255) NOT NULL,
    modelId         VARCHAR(255) NOT NULL,
    systemPromptId  INT,
    workspace       VARCHAR(1024) NOT NULL DEFAULT '',
    createdAt       VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    updatedAt       VARCHAR(64) NOT NULL DEFAULT (DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%M:%SZ')),
    PRIMARY KEY (agentId),
    UNIQUE KEY uk_route (route),
    KEY idx_modelId (modelId),
    KEY idx_systemPromptId (systemPromptId),
    CONSTRAINT fk_agent_model FOREIGN KEY (modelId) REFERENCES model_config(modelId) ON DELETE RESTRICT,
    CONSTRAINT fk_agent_prompt FOREIGN KEY (systemPromptId) REFERENCES system_prompt(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Agent association tables (many-to-many)
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_tool (
    agentId VARCHAR(255) NOT NULL,
    toolId  VARCHAR(255) NOT NULL,
    PRIMARY KEY (agentId, toolId),
    CONSTRAINT fk_at_agent FOREIGN KEY (agentId) REFERENCES agent_config(agentId) ON DELETE RESTRICT,
    CONSTRAINT fk_at_tool  FOREIGN KEY (toolId)  REFERENCES tool_config(toolId)  ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_skill (
    agentId VARCHAR(255) NOT NULL,
    skillId VARCHAR(255) NOT NULL,
    PRIMARY KEY (agentId, skillId),
    CONSTRAINT fk_as_agent FOREIGN KEY (agentId) REFERENCES agent_config(agentId) ON DELETE RESTRICT,
    CONSTRAINT fk_as_skill FOREIGN KEY (skillId) REFERENCES skill_config(skillId) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_mcp (
    agentId VARCHAR(255) NOT NULL,
    mcpId   VARCHAR(255) NOT NULL,
    PRIMARY KEY (agentId, mcpId),
    CONSTRAINT fk_am_agent FOREIGN KEY (agentId) REFERENCES agent_config(agentId) ON DELETE RESTRICT,
    CONSTRAINT fk_am_mcp   FOREIGN KEY (mcpId)   REFERENCES mcp_config(mcpId)    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
