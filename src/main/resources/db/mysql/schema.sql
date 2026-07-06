-- Yansen configuration database schema (MySQL)
-- FUTURE: not used at runtime yet — ConfigStoreFactory supports SQLite only.
-- Reserved DDL for planned MySQL ConfigStore; do not point database.type=mysql until implemented.
-- All tables use CREATE TABLE IF NOT EXISTS for idempotent execution.
-- Referential integrity is enforced in application code, not via FOREIGN KEY constraints.

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
    KEY idx_systemPromptId (systemPromptId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Agent association tables (many-to-many)
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_tool (
    agentId VARCHAR(255) NOT NULL,
    toolId  VARCHAR(255) NOT NULL,
    PRIMARY KEY (agentId, toolId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_skill (
    agentId VARCHAR(255) NOT NULL,
    skillId VARCHAR(255) NOT NULL,
    PRIMARY KEY (agentId, skillId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_mcp (
    agentId VARCHAR(255) NOT NULL,
    mcpId   VARCHAR(255) NOT NULL,
    PRIMARY KEY (agentId, mcpId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
