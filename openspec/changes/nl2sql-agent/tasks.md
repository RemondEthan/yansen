## 1. SQLite配置层

- [x] 1.1 新增SQLite JDBC依赖到pom.xml（org.xerial:sqlite-jdbc）
- [x] 1.2 编写schema.sql：model_config、system_prompt、tool_config、skill_config、mcp_config、agent_config（含route字段）、agent_tool、agent_skill、agent_mcp建表DDL
- [x] 1.3 编写init-data.sql：默认model（apiKey写${MINIMAX_API_KEY:}占位符）、默认prompt、默认tool、默认skill、默认agent（route="/api/chat"）
- [x] 1.4 定义数据类型record：ModelConfigRecord、SystemPromptRecord、ToolConfigRecord、SkillConfigRecord、McpConfigRecord、AgentConfigRecord
- [x] 1.5 实现ConfigStore接口
- [x] 1.6 实现SqliteConfigStore：JDBC连接管理、schema.sql执行、init-data.sql原样执行（占位符字面量入库）、WAL模式开启、全部CRUD操作
- [x] 1.7 实现system prompt解析：inline/file/classpath三种source_type
- [x] 1.8 实现agent关联表CRUD：agent_tool、agent_skill、agent_mcp的读写
- [x] 1.9 实现删除校验：model被agent引用时返回409、prompt被agent引用时返回409

## 2. YansenSettings重构：配置分层

- [x] 2.1 YansenSettings移除models、agents、skills、mcpServers字段，仅保留server + database
- [x] 2.2 YansenConfig.load()仅加载server + database段
- [x] 2.3 删除AgentSettings.java（被AgentConfigRecord替代）；保留ModelSettings.java（仍作为ModelProvider SPI契约类型，ModelRegistry.create()接受）
- [x] 2.4 删除SkillsConfig.java；SkillRegistry.fromSkillRecords(List<SkillConfigRecord>)，classpath类型skill预注册，workspace类型resolve时按需构建
- [x] 2.5 McpRegistry.fromMcpRecords(List<McpConfigRecord>)，反序列化McpConfigRecord.config为McpServerConfig
- [x] 2.6 yansen.yml移除models、agents、skills、mcp段，仅保留server + database段

## 3. Agent抽象与实现

- [x] 3.1 定义YansenAgent接口：agentId()、chat()、chatStream()、close()
- [x] 3.2 实现YansenAgentImpl：从ConfigStore record读取配置构建HarnessAgent；skill/mcp通过Registry resolve
- [x] 3.3 实现AgentFactory：根据AgentConfigRecord.agentType创建YansenAgent实例（当前统一用YansenAgentImpl）
- [x] 3.4 实现ModelConfigRecord→ModelSettings转换方法（运行时通过ConfigValueResolver解析占位符）

## 4. RouteRegistry + AgentRegistry

- [x] 4.1 实现RouteRegistry：ConcurrentHashMap<path, agentId>映射，register/unregister/resolve方法，保留路由冲突校验
- [x] 4.2 实现AgentRegistry：ConcurrentHashMap<agentId, YansenAgent>缓存，lazy加载，常驻内存不淘汰
- [x] 4.3 实现get(agentId)：缓存命中→返回；未命中→从ConfigStore读配置→AgentFactory实例化→放入缓存
- [x] 4.4 实现invalidate(agentId)：移除缓存实例并close释放资源
- [x] 4.5 实现listCached()：返回缓存快照
- [x] 4.6 实现close()：shutdown时清理所有缓存实例

## 5. Controller改造

- [x] 5.1 ChatRequest保持{prompt, userId, sessionId}，无agentId字段
- [x] 5.2 提取SseSession内部类：封装SSE生命周期
- [x] 5.3 实现AgentChatHandler：从RouteRegistry解析agentId→AgentRegistry.get→agent.chat
- [x] 5.4 实现AgentStreamHandler：SSE流式版本
- [x] 5.5 启动时从ConfigStore读取所有agent_config，按route动态注册Javalin端点
- [x] 5.6 删除YansenAgentService类

## 6. 配置CRUD API

- [x] 6.1 实现ConfigController：model配置CRUD端点（POST/GET/PUT/DELETE /api/config/model）
- [x] 6.2 实现ConfigController：agent配置CRUD端点，更新/删除时invalidate+RouteRegistry更新
- [x] 6.3 实现ConfigController：prompt CRUD端点
- [x] 6.4 实现ConfigController：tool CRUD端点
- [x] 6.5 实现ConfigController：skill CRUD端点
- [x] 6.6 实现ConfigController：mcp CRUD端点
- [x] 6.7 实现请求DTO：ModelConfigRequest、AgentConfigRequest、PromptConfigRequest、ToolConfigRequest、SkillConfigRequest、McpConfigRequest

## 7. 启动集成

- [x] 7.1 AgentContext重构：bootstrap()改为ConfigStore初始化→SkillRegistry/McpRegistry from records
- [x] 7.2 AgentMain重构：初始化ConfigStore→RouteRegistry→AgentRegistry→动态注册路由→ApiModule
- [x] 7.3 ApiModule重构：注册ConfigController
- [x] 7.4 HealthController扩展：GET /api/health返回registry状态
- [x] 7.5 shutdown hook中增加registry.close()和configStore.close()

## 8. 测试

- [ ] 8.1 ChatController回归测试（前置5.x）：锁定当前/api/chat同步响应和/api/chat/stream SSE行为
- [ ] 8.2 SqliteConfigStoreTest：建表、init-data占位符字面量入库、WAL模式、连接管理
- [x] 8.3 ModelConfig CRUD测试（ConfigControllerTest部分覆盖）
- [x] 8.4 AgentConfig CRUD测试（ConfigControllerTest部分覆盖）
- [ ] 8.5 SystemPrompt CRUD + resolvePromptContent测试
- [ ] 8.6 Tool/Skill/Mcp CRUD测试
- [ ] 8.7 AgentRegistryTest：lazy加载、缓存命中、invalidate、agentId不存在
- [ ] 8.8 RouteRegistryTest：register、unregister、resolve、保留路由冲突校验
- [ ] 8.9 YansenAgentImplTest：构建、chat、chatStream、systemPrompt解析
- [ ] 8.10 SseSessionTest：heartbeat调度、timeout终止、连接关闭检测
- [ ] 8.11 AgentChatHandlerTest：route路由、多轮对话
- [ ] 8.12 AgentStreamHandlerTest：SSE事件序列、heartbeat、timeout
- [x] 8.13 ConfigControllerTest：model/agent CRUD、占位符字面量入库、invalidate、409/404
- [ ] 8.14 ChatController回归验证：重构后重新运行8.1基线测试

## 9. NL2SQL Agent（待配置）

- [x] 9.1 通过CRUD API或init-data添加route="/api/nl2sql"的agent配置及专用system prompt
