## ADDED Requirements

### Requirement: Model配置数据库存储
系统SHALL将model配置存储在SQLite的model_config表中，包含：modelId、provider、modelName、baseUrl、apiKey、maxRetries、connectTimeoutSeconds、readTimeoutSeconds、writeTimeoutSeconds。

#### Scenario: 启动时初始化model_config表
- **WHEN** 系统启动，model_config表不存在
- **THEN** 执行schema.sql自动建表；若表为空则执行init-data.sql插入默认数据

#### Scenario: 从数据库读取model配置
- **WHEN** 实例化agent需要引用某个model
- **THEN** 从model_config表按modelId读取配置，构建ModelSettings

### Requirement: Agent配置数据库存储
系统SHALL将agent配置存储在SQLite的agent_config表中，包含：agentId、name、agentType（default/nl2sql）、route（URL路径，UNIQUE约束）、modelId（引用model_config）、systemPromptId（引用system_prompt）、workspace。agent与tool/skill/mcp的关联通过agent_tool、agent_skill、agent_mcp关联表管理。

#### Scenario: 启动时初始化agent_config表
- **WHEN** 系统启动，agent_config表不存在
- **THEN** 执行schema.sql自动建表；若表为空则执行init-data.sql插入默认agent

#### Scenario: 从数据库读取agent配置
- **WHEN** lazy加载agent实例
- **THEN** 从agent_config表按agentId读取配置，从关联表读取tools/skills/mcp列表，根据agentType实例化对应的Agent类

### Requirement: System Prompt数据库存储
系统SHALL将system prompt存储在SQLite的system_prompt表中，包含：id、name、sourceType（inline/file/classpath）、sourceRef。agent_config通过system_prompt_id外键引用。

#### Scenario: 解析inline prompt
- **WHEN** sourceType="inline"
- **THEN** 直接返回sourceRef作为prompt正文

#### Scenario: 解析file prompt
- **WHEN** sourceType="file"
- **THEN** 读取sourceRef路径对应的文件内容作为prompt正文

#### Scenario: 解析classpath prompt
- **WHEN** sourceType="classpath"
- **THEN** 读取classpath资源sourceRef作为prompt正文

### Requirement: Tool配置数据库存储
系统SHALL将tool配置存储在SQLite的tool_config表中，包含：toolId、name、description、enabled。agent通过agent_tool关联表引用tool。

#### Scenario: 从数据库读取agent的tools
- **WHEN** lazy加载agent实例
- **THEN** 从agent_tool关联表读取该agent的toolId列表，仅包含enabled=1的tool

### Requirement: Skill配置数据库存储
系统SHALL将skill配置存储在SQLite的skill_config表中，包含：skillId、name、sourceType（classpath/workspace）、sourceRef。agent通过agent_skill关联表引用skill。

#### Scenario: 从数据库读取agent的skills
- **WHEN** lazy加载agent实例
- **THEN** 从agent_skill关联表读取该agent的skillId列表，解析sourceType和sourceRef构建AgentSkillRepository

### Requirement: MCP配置数据库存储
系统SHALL将mcp配置存储在SQLite的mcp_config表中，包含：mcpId、name、config（JSON序列化的McpServerConfig）。agent通过agent_mcp关联表引用mcp。

#### Scenario: 从数据库读取agent的mcp
- **WHEN** lazy加载agent实例
- **THEN** 从agent_mcp关联表读取该agent的mcpId列表，反序列化config为McpServerConfig

### Requirement: SQLite通过SQL脚本初始化
系统SHALL通过classpath中的SQL脚本初始化SQLite数据库。schema.sql负责建表（幂等），init-data.sql负责插入默认数据（仅在表空时执行）。不存在YAML迁移逻辑。

#### Scenario: 首次启动初始化
- **WHEN** 系统启动，db文件不存在
- **THEN** 创建db文件，执行schema.sql建表，对init-data.sql执行占位符预渲染后插入默认数据，开启WAL模式

#### Scenario: 非首次启动
- **WHEN** 系统启动，db文件存在且表已有数据
- **THEN** 跳过初始化，直接使用数据库中的配置

### Requirement: init-data.sql占位符存储
系统SHALL将init-data.sql中的`${ENV_VAR:default}`占位符以字面量形式写入SQLite，不做环境变量预渲染。占位符写在SQL字符串字面量内（保持SQL语法合法）。env变量替换仅在agent实例化时通过ConfigValueResolver.resolveStored()执行。CRUD API写入的值同样不做占位符解析。

#### Scenario: init-data.sql占位符字面量入库
- **WHEN** init-data.sql包含`'${MINIMAX_API_KEY:}'`
- **THEN** SQLite model_config.apiKey存储字面量`${MINIMAX_API_KEY:}`，不做环境变量替换

#### Scenario: 实例化agent时解析占位符
- **WHEN** agent引用model_config.apiKey='${MINIMAX_API_KEY:}'，环境变量MINIMAX_API_KEY=sk-xxx
- **THEN** ModelConfigRecord.toModelSettings()解析为sk-xxx供ModelRegistry.create()使用

#### Scenario: 环境变量不存在时使用默认值
- **WHEN** apiKey='${MINIMAX_API_KEY:}'，环境变量MINIMAX_API_KEY未设置
- **THEN** 实例化时解析为空串（冒号后为空即默认值为空串）

#### Scenario: 无占位符的值不受影响
- **WHEN** init-data.sql包含`'openai-compatible'`（不含${...}）
- **THEN** 值不变，仍为`'openai-compatible'`

#### Scenario: CRUD API写入的值不做占位符解析
- **WHEN** 通过PUT /api/config/model/{id}写入apiKey='${SOME_KEY}'
- **THEN** SQLite中存储字面量`${SOME_KEY}`，不做环境变量替换

### Requirement: Model配置CRUD API
系统SHALL提供model配置的CRUD API：`POST /api/config/model`（创建）、`GET /api/config/model`（列表）、`GET /api/config/model/{id}`（查询）、`PUT /api/config/model/{id}`（更新）、`DELETE /api/config/model/{id}`（删除）。

#### Scenario: 创建model配置
- **WHEN** 发送`POST /api/config/model`，body包含provider、modelName、baseUrl、apiKey
- **THEN** 写入model_config表，返回201

#### Scenario: 更新model配置
- **WHEN** 发送`PUT /api/config/model/{id}`，body包含新的apiKey
- **THEN** 更新model_config表对应记录，返回200

#### Scenario: 删除model配置
- **WHEN** 发送`DELETE /api/config/model/{id}`，该model未被任何agent引用
- **THEN** 删除model_config表对应记录，返回204

#### Scenario: 删除被引用的model配置
- **WHEN** 发送`DELETE /api/config/model/{id}`，该model被agent_config引用
- **THEN** 返回409，提示存在引用的agent

### Requirement: Agent配置CRUD API
系统SHALL提供agent配置的CRUD API：`POST /api/config/agent`（创建）、`GET /api/config/agent`（列表）、`GET /api/config/agent/{id}`（查询）、`PUT /api/config/agent/{id}`（更新）、`DELETE /api/config/agent/{id}`（删除）。

#### Scenario: 创建agent配置
- **WHEN** 发送`POST /api/config/agent`，body包含agentId、agentType、route、modelId、systemPromptId、tools、skills、mcp
- **THEN** 写入agent_config表及关联表，注册路由，返回201

#### Scenario: 更新agent配置触发重新加载
- **WHEN** 发送`PUT /api/config/agent/{id}`，更新systemPromptId
- **THEN** 更新agent_config表及关联表，使Registry中该agentId的缓存实例失效，若route变更则更新RouteRegistry，返回200

#### Scenario: 删除agent配置
- **WHEN** 发送`DELETE /api/config/agent/{id}`
- **THEN** 删除agent_config表记录及关联表记录，使Registry中该agentId的缓存实例失效，标记路由失效（handler返回404），返回204

### Requirement: System Prompt CRUD API
系统SHALL提供system prompt的CRUD API：`POST /api/config/prompt`（创建）、`GET /api/config/prompt`（列表）、`GET /api/config/prompt/{id}`（查询）、`PUT /api/config/prompt/{id}`（更新）、`DELETE /api/config/prompt/{id}`（删除）。

#### Scenario: 删除被引用的prompt
- **WHEN** 发送`DELETE /api/config/prompt/{id}`，该prompt被agent_config引用
- **THEN** 返回409，提示存在引用的agent

### Requirement: Tool/Skill/MCP CRUD API
系统SHALL提供tool、skill、mcp配置的CRUD API，路径分别为`/api/config/tool`、`/api/config/skill`、`/api/config/mcp`。

### Requirement: YAML仅保留服务配置
系统SHALL确保yansen.yml仅包含server配置段。models、agents、skills、mcp段从yansen.yml中移除。YansenSettings仅包含ServerSettings。
