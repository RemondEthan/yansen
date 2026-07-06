## Context

Yansen当前是一个基于agentscope-harness的通用对话agent服务，通过Javalin HTTP server对外提供chat/stream API。

### 当前架构

```
yansen.yml (models + agents + server + skills + mcp)
       ↓
  AgentContext.bootstrap()
       ↓
  YansenConfig.load() → YansenSettings (全部从YAML)
       ↓
  YansenAgentService (单HarnessAgent实例, 硬编码取default)
       ↓
  ChatController → 单agentService
```

关键现状：
- `YansenSettings`包含server、models、agents、skills、mcpServers，全部从yansen.yml加载
- 只实例化一个default agent（`settings.defaultAgent()`）
- `HarnessAgent`的system prompt在`builder().sysPrompt()`时绑定，不支持per-request覆盖
- model、agent、tool、skill、mcp配置写死在YAML中，无法运行时修改

### 目标架构

```
yansen.yml (server)                  SQLite (全部业务配置)
       ↓                                    ↓
  ServerSettings                        ConfigStore
       ↓                                    ↓
  AgentContext.bootstrap() ──────────────→ ConfigStore.open(dataDir)
       ↓
  RouteRegistry ← agent_config.route
       ↓
  AgentRegistry (lazy加载, 常驻内存, 不淘汰)
       ↓
  动态注册的Handler → registry.get(agentId) → agent.chat(prompt)
```

## Goals / Non-Goals

**Goals:**
- 实现SQLite配置存储：model_config、agent_config、system_prompt、tool_config、skill_config、mcp_config及agent关联表
- SQLite通过SQL脚本初始化（schema.sql + init-data.sql），不存在YAML迁移逻辑
- yansen.yml仅保留基础服务配置（server）
- 实现RouteRegistry：agent_config.route字段驱动，启动时动态注册Javalin端点，URL路径即路由
- 实现AgentRegistry，请求到来时lazy加载agent实例（读SQLite配置→实例化→缓存），加载后常驻内存，不做淘汰
- Agent数量由SQLite配置决定，固定不变；只有配置过的agent才可以被加载
- 实现配置CRUD API（model/agent/prompt/tool/skill/mcp），agent配置更新后invalidate缓存实例
- ChatRequest无agentId字段，路由由URL路径决定
- 支持多轮对话（基于sessionId，对话历史由HarnessAgent的session memory在内存中维护）

**Non-Goals:**
- 不做YAML→SQLite自动迁移（配置通过SQL脚本或CRUD API管理）
- 不做agent淘汰/LRU淘汰（agent数量固定，加载后常驻内存）
- 不做session持久化到磁盘（agent不淘汰，HarnessAgent自身维护对话历史，无需持久化）
- 不做配置的版本管理/审计日志（后续可加）
- 不做NL→DSL→SQL两步路径（仅做NL→SQL直出）
- 不做SQL执行引擎（仅生成SQL，不执行）

## Decisions

### 1. 配置职责划分：YAML管服务，SQLite管业务

**选择**：

| 配置项 | 存储位置 | 理由 |
|---|---|---|
| server（端口、超时、heartbeat） | yansen.yml | 基础设施配置，修改需重启 |
| models（provider、baseUrl、apiKey等） | SQLite | 业务配置，需运行时增删改 |
| agents（type、route、model引用、systemPrompt、workspace等） | SQLite | 业务配置，需运行时增删改 |
| system_prompt（内容、来源类型） | SQLite | 业务配置，需运行时增删改 |
| tools（id、名称、启用状态） | SQLite | 业务配置，需运行时增删改 |
| skills（id、来源类型、路径） | SQLite | 业务配置，需运行时增删改 |
| mcp（id、server配置） | SQLite | 业务配置，需运行时增删改 |

**理由**：基础服务配置修改频率低且需重启生效，适合文件；业务配置需要运行时管理（CRUD API），适合数据库。两者职责不交叉，不存在"空库去YAML读"的场景。

### 2. SQLite初始化：SQL脚本，占位符字面量入库

**选择**：SQLite通过classpath中的SQL脚本初始化：
- `schema.sql` — 建表DDL（`CREATE TABLE IF NOT EXISTS`，幂等）
- `init-data.sql` — 默认数据（default model + default prompt + default agent）

启动逻辑：表不存在→执行schema.sql；agent_config表空→执行init-data.sql。

**占位符存储**：`init-data.sql`中的字符串值可包含`${ENV_VAR:default}`占位符（写在SQL字符串字面量内，保持SQL语法合法）。占位符以字面量形式写入SQLite，**不做**插入时的环境变量预渲染。env替换在agent实例化时通过`ConfigValueResolver.resolveStored()`执行（`ModelConfigRecord.toModelSettings()`、agent workspace等）。示例：

```sql
INSERT INTO model_config (modelId, provider, modelName, baseUrl, apiKey, ...)
VALUES ('default', 'openai-compatible', 'MiniMax-M3', 'https://api.minimaxi.com/v1', '${MINIMAX_API_KEY:}', ...);
```

CRUD API写入的值同样不做占位符解析（存什么是什么）。

**替代方案**：
- (a) init-data.sql执行前预渲染占位符——env值固化到DB，换密钥需改库；与"配置可移植"目标冲突
- (b) init-data.sql硬编码空串，启动后手动CRUD设置apiKey——首次启动agent不可用

**理由**：占位符存库、运行时解析，与yansen.yml占位符语法一致，且密钥变更只需重启/重新实例化agent，无需改库。不存在YAML迁移逻辑。

### 3. Agent是工程内的类，配置驱动实例化

**选择**：当前所有agentType行为相同（封装HarnessAgent），用`YansenAgentImpl`统一实现。agentType仅存于配置，运行时行为无差异。未来NL2SQL需要SQL后处理时，在`AgentFactory`中新增分支。

**替代方案**：拆DefaultAgent/Nl2sqlAgent两个类——当前行为完全相同，过早拆分增加维护成本。

**理由**：agent作为工程内的类，可以有类型特定的行为（如NL2SQLAgent未来可加SQL后处理），同时配置驱动实例化保持灵活性。当前不拆，按需拆分。

### 4. Route-based路由：URL路径即路由，消除agentId硬编码

**选择**：agent_config增加`route`字段（TEXT NOT NULL UNIQUE），启动时从DB读取所有agent的route，动态注册Javalin端点。请求到达时，URL路径直接映射到agentId。

```
agent_config示例：
  agentId="default",           route="/api/chat"       → POST /api/chat, GET /api/chat/stream
  agentId="nl2sql-contract",   route="/api/nl2sql"     → POST /api/nl2sql, GET /api/nl2sql/stream
```

RouteRegistry维护path→agentId映射。请求时`routeRegistry.resolve(path)` → `agentRegistry.get(agentId)` → lazy load。

**替代方案**：ChatRequest携带agentId字段——客户端硬编码agentId字符串，与配置耦合，不够优雅。

**理由**：URL路径是REST的天然路由方式，客户端只关心URL不关心内部agentId。新增agent只需在DB插入配置+route，重启后自动注册。ChatRequest保持干净的`{prompt, userId, sessionId}`。

**路由注销**：Javalin无官方路由注销API。运行时删除agent配置时，采用"标记失效"策略——路由entry保留，handler内部查RouteRegistry，若route已删除则返回404。死路由留在handler list中，agent数量级为几十，无内存问题。已建立的SSE流不受影响（连接已脱离路由匹配阶段）。

### 5. Lazy加载：请求到来时按需实例化，常驻内存不淘汰

**选择**：AgentRegistry维护`ConcurrentHashMap<String, YansenAgent>`。`get(agentId)`时若缓存未命中，从SQLite读取配置，实例化agent，放入缓存。加载后常驻内存，不做淘汰。

**替代方案**：
- (a) 启动时全量加载所有agent——无法控制启动时间，且agent数量可能增长
- (b) LRU淘汰——引入淘汰后session恢复问题（replay机制有LLM开销+非确定性问题），复杂度高

**理由**：lazy加载按需实例化，启动快。agent数量由配置决定，固定不变，不需要淘汰。HarnessAgent自身在内存中维护session对话历史，agent不被淘汰则历史不丢。配置变更后invalidate → 下次请求按新配置重新实例化 = 热加载。

### 6. 配置CRUD API

**选择**：提供REST API管理全部业务配置：

```
POST   /api/config/model          创建model配置
GET    /api/config/model          列表
GET    /api/config/model/{id}     查询
PUT    /api/config/model/{id}     更新
DELETE /api/config/model/{id}     删除

POST   /api/config/agent          创建agent配置
GET    /api/config/agent          列表
GET    /api/config/agent/{id}     查询
PUT    /api/config/agent/{id}     更新（同时invalidate缓存+标记路由失效）
DELETE /api/config/agent/{id}     删除（同时invalidate缓存+标记路由失效）

POST   /api/config/prompt         创建prompt
GET    /api/config/prompt         列表
GET    /api/config/prompt/{id}    查询
PUT    /api/config/prompt/{id}    更新
DELETE /api/config/prompt/{id}    删除

POST   /api/config/tool           创建tool
GET    /api/config/tool           列表
PUT    /api/config/tool/{id}      更新
DELETE /api/config/tool/{id}      删除

POST   /api/config/skill          创建skill
GET    /api/config/skill          列表
PUT    /api/config/skill/{id}     更新
DELETE /api/config/skill/{id}     删除

POST   /api/config/mcp            创建mcp
GET    /api/config/mcp            列表
PUT    /api/config/mcp/{id}       更新
DELETE /api/config/mcp/{id}       删除
```

**理由**：配置在SQLite中，需要CRUD API供外部系统/运维管理。agent配置更新后invalidate缓存，下次请求按新配置lazy重新实例化。agent路由变更时同步更新RouteRegistry（标记失效+新增注册）。

### 7. System Prompt独立管理

**选择**：system_prompt是SQLite中的一等实体，三种source_type：

| source_type | source_ref含义 | 解析逻辑 |
|-------------|---------------|----------|
| `inline` | prompt正文直接存储 | 直接返回 |
| `file` | 文件系统路径 | `Files.readString(Path.of(sourceRef))` |
| `classpath` | classpath资源路径 | 读classpath资源 |

agent_config.system_prompt_id是FK，一个prompt可被多个agent复用。

**理由**：prompt独立于agent存在，支持复用和独立管理。三种source_type覆盖所有使用场景。

## Risks / Trade-offs

- [LLM生成SQL的正确性无法100%保证] → system prompt中定义严格规范约束输出质量，未来可引入NL→DSL→SQL两步路径提升准确率
- [SQLite并发写入限制] → 配置写入仅在CRUD API中，频率极低；WAL模式支持并发读
- [配置更新后已缓存agent实例不立即生效] → lazy加载天然支持：invalidate旧实例后下次请求读新配置重新实例化
- [yansen.yml移除models/agents/skills/mcp是BREAKING变更] → 通过init-data.sql提供默认配置，开箱即用
- [ChatController重构影响现有功能] → /api/chat行为不变（route="/api/chat"映射到default agent），先写测试验证
- [Javalin运行时路由注销无官方API] → 采用"标记失效"策略，handler内查RouteRegistry有效性，无效则404
- [agent常驻内存的内存占用] → 每个agent实例约几十MB（主要是Model对象），10个agent约几百MB，生产环境可控；若agent数量增长到50+需关注
