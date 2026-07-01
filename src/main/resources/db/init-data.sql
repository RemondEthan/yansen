-- Yansen default configuration data
-- Inserted only when agent_config table is empty (first startup).
-- String values may contain ${ENV_VAR:default} placeholders — SqliteConfigStore
-- pre-renders them via PlaceholderResolver before execution.

-- Default model (MiniMax-M3 via openai-compatible provider)
INSERT INTO model_config (modelId, provider, modelName, baseUrl, apiKey, maxRetries, connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds)
VALUES ('default', 'openai-compatible', 'MiniMax-M3', 'https://api.minimaxi.com/v1', '${MINIMAX_API_KEY:}', 3, 30, 900, 30);

-- Default system prompt (classpath resource)
INSERT INTO system_prompt (name, sourceType, sourceRef)
VALUES ('default', 'classpath', 'system-prompt.md');

-- Default tool
INSERT INTO tool_config (toolId, name, description, enabled)
VALUES ('datetime', 'current_datetime', 'Returns the current local date-time in ISO-8601 format', 1);

-- Default skill (classpath)
INSERT INTO skill_config (skillId, name, sourceType, sourceRef)
VALUES ('default', 'Default skills', 'classpath', 'skills');

-- Default agent (route=/api/chat, references default model + prompt + tool + skill)
-- NOTE: systemPromptId=1 relies on AUTOINCREMENT starting at 1 for the first
-- INSERT into system_prompt above. This is safe because init-data.sql runs only
-- when agent_config is empty (first startup), so system_prompt is also empty.
INSERT INTO agent_config (agentId, name, agentType, route, modelId, systemPromptId, workspace)
VALUES ('default', 'Default agent', 'default', '/api/chat', 'default', 1, '${YANSEN_WORKSPACE:}');

-- Agent-tool association
INSERT INTO agent_tool (agentId, toolId)
VALUES ('default', 'datetime');

-- Agent-skill association
INSERT INTO agent_skill (agentId, skillId)
VALUES ('default', 'default');
