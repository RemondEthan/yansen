## 1. SQLite配置层

- [x] 1.1 新增SQLite JDBC依赖到pom.xml（org.xerial:sqlite-jdbc）
- [x] 1.2 编写schema.sql：model_config、system_prompt、tool_config、skill_config、mcp_config、agent_config（含route字段）、agent_tool、agent_skill、agent_mcp建表DDL
- [x] 1.3 编写init-data.sql：默认model（apiKey写${MINIMAX_API_KEY:}占位符）、默认prompt、默认tool、默认skill、默认agent（route="/api/chat"）
- [x] 1.4 定义数据类型record：ModelConfigRecord、SystemPromptRecord、ToolConfigRecord、SkillConfigRecord、McpConfigRecord、AgentConfigRecord
- [x] 1.5 实现ConfigStore接口
- [ ] 1.6 实现SqliteConfigStore：JDBC连接管理、schema.sql执行、init-data.sql占位符预渲染（逐行PlaceholderResolver.resolve()）后执行、WAL模式开启、全部CRUD操作
- [ ] 1.7 实现system prompt解析：inline/file/classpath三种source_type
- [ ] 1.8 实现agent关联表CRUD：agent_tool、agent_skill、agent_mcp的读写
- [ ] 1.9 实现删除校验：model被agent引用时返回409、prompt被agent引用时返回409

## 2. YansenSettings重构：配置分层

- [ ] 2.1 YansenSettings移除models、agents、skills、mcpServers字段，仅保留server
- [ ] 2.2 YansenConfig.load()仅加载server段
- [ ] 2.3 删除AgentSettings.java（被AgentConfigRecord替代）；保留ModelSettings.java（仍作为ModelProvider SPI契约类型，ModelRegistry.create()接受）
- [ ] 2.4 删除SkillsConfig.java；SkillRegistry.fromClasspathConfig(SkillsConfig)替换为fromSkillRecords(List<SkillConfigRecord>)，classpath类型skill预注册，workspace类型resolve时按需构建（保持现有行为）
- [ ] 2.5 McpRegistry.fromSettings(Map)替换为fromMcpRecords(List<McpConfigRecord>)，反序列化McpConfigRecord.config为McpServerConfig
- [ ] 2.6 yansen.yml移除models、agents、skills、mcp段，仅保留server段

## 3. Agent抽象与实现

- [ ] 3.1 定义YansenAgent接口：agentId()、chat()、chatStream()、close()
- [ ] 3.2 实现YansenAgentImpl：从YansenAgentService迁移HarnessAgent构建逻辑，构造参数从ConfigStore record读取；skill通过SkillRegistry.resolve(agentSkillIds, workspace)获取，mcp通过McpRegistry.resolve(agentMcpIds)获取
- [ ] 3.3 实现AgentFactory：根据AgentConfigRecord.agentType创建YansenAgent实例（当前所有类型统一用YansenAgentImpl）
- [ ] 3.4 实现ModelConfigRecord→ModelSettings转换方法（ModelRegistry.create()仍接受ModelSettings）

## 4. RouteRegistry + AgentRegistry

- [ ] 4.1 实现RouteRegistry：ConcurrentHashMap<path, agentId>映射，register/unregister/resolve方法，保留路由冲突校验（系统保留路由+已注册route）
- [ ] 4.2 实现AgentRegistry：ConcurrentHashMap<agentId, YansenAgent>缓存，lazy加载，常驻内存不淘汰
- [ ] 4.3 实现get(agentId)：缓存命中→返回；未命中→从ConfigStore读配置→AgentFactory实例化→放入缓存
- [ ] 4.4 实现invalidate(agentId)：移除缓存实例并close释放资源
- [ ] 4.5 实现listCached()：返回缓存快照
- [ ] 4.6 实现close()：shutdown时清理所有缓存实例

## 5. Controller改造

- [ ] 5.1 ChatRequest保持{prompt, userId, sessionId}，无agentId字段
- [ ] 5.2 提取SseSession内部类：封装SSE生命周期（keepAlive/onClose/completedByApp标记）、heartbeat调度与取消、synchronized写入（safeSendEvent/safeSendComment）、Flux订阅与终止（terminateWithDone/terminateWithError）、连接关闭检测（appInitiated vs 外部断开）。依赖SseEventMapper（构造注入，可替换）
- [ ] 5.3 实现AgentChatHandler：从RouteRegistry解析agentId→AgentRegistry.get→agent.chat→返回ChatResponse
- [ ] 5.4 实现AgentStreamHandler：SSE流式版本，从RouteRegistry解析agentId→AgentRegistry.get→agent.chatStream→构造SseSession→订阅Flux→SseSession转发事件
- [ ] 5.5 启动时从ConfigStore读取所有agent_config，按route动态注册Javalin端点（post + sse）
- [ ] 5.6 删除YansenAgentService类（职责由AgentRegistry+AgentFactory+YansenAgentImpl替代）

## 6. 配置CRUD API

- [ ] 6.1 实现ConfigController：model配置CRUD端点（POST/GET/PUT/DELETE /api/config/model）
- [ ] 6.2 实现ConfigController：agent配置CRUD端点（POST/GET/PUT/DELETE /api/config/agent），更新时调用registry.invalidate()+RouteRegistry更新，删除时标记路由失效
- [ ] 6.3 实现ConfigController：prompt CRUD端点（POST/GET/PUT/DELETE /api/config/prompt）
- [ ] 6.4 实现ConfigController：tool CRUD端点（POST/GET/PUT/DELETE /api/config/tool）
- [ ] 6.5 实现ConfigController：skill CRUD端点（POST/GET/PUT/DELETE /api/config/skill）
- [ ] 6.6 实现ConfigController：mcp CRUD端点（POST/GET/PUT/DELETE /api/config/mcp）
- [ ] 6.7 实现请求/响应DTO：ModelConfigRequest、AgentConfigRequest、PromptRequest、ToolConfigRequest、SkillConfigRequest、McpConfigRequest

## 7. 启动集成

- [ ] 7.1 AgentContext重构：移除YansenSettings中models/agents/skills/mcpServers依赖；bootstrap()改为ConfigStore初始化→SkillRegistry.fromSkillRecords(configStore.listSkills())→McpRegistry.fromMcpRecords(configStore.listMcp())；保留ToolRegistry.discover()和ModelRegistry.discover()（SPI发现不变）
- [ ] 7.2 AgentMain重构：初始化ConfigStore→初始化RouteRegistry→初始化AgentRegistry→动态注册路由→注入ApiModule
- [ ] 7.3 ApiModule重构：注册ConfigController，依赖改为registry+configStore
- [ ] 7.4 HealthController扩展：GET /api/health返回registry状态（已缓存agent数量、agentId列表、路由映射）
- [ ] 7.5 shutdown hook中增加registry.close()和configStore.close()

## 8. 测试

- [ ] 8.1 ChatController回归测试（前置5.x）：锁定当前/api/chat同步响应和/api/chat/stream SSE行为（事件序列、heartbeat、timeout、连接关闭），作为重构基线
- [ ] 8.2 SqliteConfigStoreTest：建表、init-data预渲染（环境变量替换/默认值/无占位符值不变）、WAL模式、连接管理
- [ ] 8.3 ModelConfig CRUD测试
- [ ] 8.4 AgentConfig CRUD测试（含关联表）
- [ ] 8.5 SystemPrompt CRUD + resolvePromptContent测试（inline/file/classpath）
- [ ] 8.6 Tool/Skill/Mcp CRUD测试
- [ ] 8.7 AgentRegistryTest：lazy加载、缓存命中、invalidate、agentId不存在
- [ ] 8.8 RouteRegistryTest：register、unregister、resolve、保留路由冲突校验、route冲突校验
- [ ] 8.9 YansenAgentImplTest：构建、chat、chatStream、systemPrompt解析
- [ ] 8.10 SseSessionTest：heartbeat调度、timeout终止、连接关闭检测、synchronized写入
- [ ] 8.11 AgentChatHandlerTest：route路由、多轮对话
- [ ] 8.12 AgentStreamHandlerTest：SSE事件序列、heartbeat、timeout、连接关闭
- [ ] 8.13 ConfigControllerTest：model/agent/prompt/tool/skill/mcp CRUD、agent更新触发invalidate+路由更新、删除被引用返回409
- [ ] 8.14 ChatController回归验证：重构后重新运行8.1基线测试，确认/api/chat行为不变
