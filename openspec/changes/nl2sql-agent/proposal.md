## Why

当前Yansen仅具备通用对话能力，无法针对业务场景生成可执行的数据库查询SQL。业务用户需要通过自然语言描述查询需求，系统自动生成生产可用的SQL。NL2SQL是数据分析场景的核心能力，是agent从"通用对话"走向"业务可用"的第一步。

同时，当前model、agent、tool、skill、mcp配置全部硬编码在yansen.yml中，无法动态管理。需要将全部业务配置迁移到SQLite，支持运行时增删改，而基础服务配置（端口、超时等）保留在YAML中。

## What Changes

- 新增SQLite配置存储：model、agent、system_prompt、tool、skill、mcp配置全部从SQLite读取，替代yansen.yml中的对应段
- yansen.yml仅保留基础服务配置（server），移除models、agents、skills、mcp段
- SQLite通过classpath SQL脚本初始化（schema.sql建表 + init-data.sql默认数据），init-data.sql支持${ENV_VAR:default}占位符预渲染（写在SQL字符串字面量内，保持SQL语法合法），不存在YAML迁移逻辑
- 新增RouteRegistry：agent_config.route字段驱动，启动时动态注册Javalin端点，URL路径即路由，消除agentId硬编码
- 新增AgentRegistry，请求到来时lazy加载agent实例（按agentId从SQLite读配置→实例化HarnessAgent→缓存），加载后常驻内存，不做淘汰
- Agent数量由SQLite配置决定，固定不变；只有配置过的agent才可以被加载
- 新增配置CRUD API，支持运行时管理全部业务配置（model/agent/prompt/tool/skill/mcp）
- ChatRequest无agentId字段，路由由URL路径决定
- 新增NL2SQL查询能力：通过route="/api/nl2sql"的agent配置，userPrompt透传给LLM

## Capabilities

### New Capabilities
- `config-database`: 配置数据库存储——model、agent、system_prompt、tool、skill、mcp配置全部存储在SQLite中，提供CRUD API，SQL脚本初始化（init-data.sql支持占位符预渲染）
- `agent-registry`: Agent注册中心——lazy加载+缓存agent实例，加载后常驻内存不淘汰；route-based路由；agent类型由配置驱动
- `nl2sql-agent`: NL2SQL Agent——通过route="/api/nl2sql"的agent配置接收userPrompt，路由到配置了NL2SQL专用system prompt的agent实例

### Modified Capabilities

（无现有capability需要修改）

## Impact

- 新增Java包：`com.glodon.mordor.yansen.config.store`（配置数据库）、`com.glodon.mordor.yansen.agent`（Agent抽象+工厂）、`com.glodon.mordor.yansen.registry`（Agent Registry + Route Registry）
- 新增数据库表：model_config、agent_config、system_prompt、tool_config、skill_config、mcp_config、agent_tool、agent_skill、agent_mcp
- 新增SQL脚本：schema.sql、init-data.sql
- 新增API端点：配置CRUD（/api/config/*）
- **BREAKING**：yansen.yml移除models、agents、skills、mcp段，全部迁移到SQLite
- 新增依赖：SQLite JDBC驱动
- 现有/api/chat行为不变（route="/api/chat"映射到default agent，向后兼容）
