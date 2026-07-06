-- Yansen default configuration data
-- Inserted only when agent_config table is empty (first startup).
-- Cross-table references use stable names/ids; integrity is enforced in Java, not DB FKs.
-- Placeholder expressions (${ENV_VAR:default}) are stored literally in SQLite and
-- resolved at agent instantiation time via ConfigValueResolver (not at insert time).

-- Default model (MiniMax-M3 via openai-compatible provider)
INSERT INTO model_config (modelId, provider, modelName, baseUrl, apiKey, maxRetries, connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds)
VALUES ('default', 'openai-compatible', 'MiniMax-M3', 'https://api.minimaxi.com/v1', '${MINIMAX_API_KEY:}', 3, 30, 900, 30);

-- General-purpose system prompt for POST /api/chat
INSERT INTO system_prompt (name, sourceType, sourceRef)
VALUES ('default-general', 'classpath', 'system-prompt.md');

-- NL2SQL business system prompt for POST /api/nl2sql
INSERT INTO system_prompt (name, sourceType, sourceRef)
VALUES ('nl2sql', 'classpath', 'prompts/nl2sql-system-prompt.md');

-- Default tool
INSERT INTO tool_config (toolId, name, description, enabled)
VALUES ('datetime', 'current_datetime', 'Returns the current local date-time in ISO-8601 format', 1);

-- Default skill (classpath): bundled under src/main/resources/skills/<name>/SKILL.md
INSERT INTO skill_config (skillId, name, sourceType, sourceRef)
VALUES ('default', 'Default skills', 'classpath', 'skills');

-- Custom skills (workspace): not seeded by default. Add via CRUD when files exist on disk:
--   skill_config: sourceType='workspace', sourceRef='skills'  (relative to agent workspace)
--   files at:     {YANSEN_WORKSPACE or agent workspace}/skills/<skill-name>/SKILL.md
--   link agent:   INSERT INTO agent_skill (agentId, skillId) VALUES ('default', '<skillId>');

-- Default agent: general assistant at /api/chat
INSERT INTO agent_config (agentId, name, agentType, route, modelId, systemPromptId, workspace)
VALUES (
    'default', 'Default agent', 'default', '/api/chat', 'default',
    (SELECT id FROM system_prompt WHERE name = 'default-general'),
    '${YANSEN_WORKSPACE:./agentscope}'
);

-- NL2SQL agent: business TextToSQL prompt at /api/nl2sql
INSERT INTO agent_config (agentId, name, agentType, route, modelId, systemPromptId, workspace)
VALUES (
    'nl2sql', 'NL2SQL Agent', 'nl2sql', '/api/nl2sql', 'default',
    (SELECT id FROM system_prompt WHERE name = 'nl2sql'),
    '${YANSEN_WORKSPACE:./agentscope}'
);

-- Agent-tool association (default agent only)
INSERT INTO agent_tool (agentId, toolId)
VALUES ('default', 'datetime');

-- Agent-skill association (default agent only)
INSERT INTO agent_skill (agentId, skillId)
VALUES ('default', 'default');
