## ADDED Requirements

### Requirement: Route-based路由
系统SHALL在agent_config表中维护route字段（TEXT NOT NULL UNIQUE），启动时从数据库读取所有agent的route，动态注册Javalin端点。请求到达时，URL路径直接映射到agentId，无需客户端传递agentId。

#### Scenario: 启动时动态注册路由
- **WHEN** 系统启动，agent_config表包含route="/api/chat"和route="/api/nl2sql"两条记录
- **THEN** 系统注册POST /api/chat、GET /api/chat/stream、POST /api/nl2sql、GET /api/nl2sql/stream四个端点

#### Scenario: 请求通过URL路由到agent
- **WHEN** 发送POST /api/nl2sql，body包含prompt
- **THEN** 系统通过RouteRegistry解析"/api/nl2sql"→agentId，从AgentRegistry获取/加载对应agent，处理请求

#### Scenario: route冲突
- **WHEN** 创建agent配置时route="/api/chat"，但该route已存在
- **THEN** 返回409，提示route已被占用

#### Scenario: route与系统保留路由冲突
- **WHEN** 创建agent配置时route="/api/health"或route="/api/config/model"
- **THEN** 返回409，提示route与系统保留路由冲突

#### Scenario: agent配置变更时路由更新
- **WHEN** 通过API更新agent的route从"/api/old"改为"/api/new"
- **THEN** 标记旧路由失效（handler返回404），注册新路由，invalidate缓存实例

### Requirement: Agent lazy加载，常驻内存不淘汰
系统SHALL在请求到来时按agentId lazy加载agent实例。若Registry缓存未命中，从SQLite读取AgentConfig，实例化对应类型的agent，放入缓存。加载后常驻内存，不做淘汰。Agent数量由SQLite配置决定，固定不变。

#### Scenario: 首次请求触发lazy加载
- **WHEN** 请求路由到agentId="nl2sql-contract"，Registry中无该agent缓存
- **THEN** 系统从SQLite读取agentId对应的AgentConfig及关联的model/prompt/tools/skills/mcp，实例化YansenAgentImpl，放入Registry缓存，处理请求

#### Scenario: 后续请求命中缓存
- **WHEN** 请求路由到agentId="nl2sql-contract"，Registry中已有该agent缓存
- **THEN** 直接使用缓存中的agent实例处理请求，不访问数据库

#### Scenario: 未配置的agentId请求失败
- **WHEN** 请求路由到的agentId在SQLite agent_config表中不存在
- **THEN** 返回400，提示"Agent not found"

### Requirement: Agent配置变更触发重新加载
系统SHALL在agent配置更新后，使Registry中已缓存的该agent实例失效。下次请求时按新配置lazy重新实例化。

#### Scenario: 更新agent配置后重新加载
- **WHEN** 通过API更新agentId="nl2sql-contract"的systemPromptId，随后收到该agentId的请求
- **THEN** 系统淘汰Registry中旧实例，按新配置重新实例化，处理请求

#### Scenario: 删除agent配置后请求返回404
- **WHEN** 通过API删除agentId="nl2sql-contract"的配置，随后收到该route的请求
- **THEN** 路由handler标记失效，返回404

### Requirement: 多轮对话由HarnessAgent session memory维护
系统SHALL依赖HarnessAgent自身的session memory维护多轮对话历史。agent常驻内存不被淘汰，对话历史在内存中持续可用，无需额外持久化机制。

#### Scenario: 同一sessionId多轮对话
- **WHEN** 用户在同一route+sessionId中发送多轮prompt
- **THEN** HarnessAgent基于内存中的session memory维护对话历史，每轮对话基于完整历史上下文
