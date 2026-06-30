## ADDED Requirements

### Requirement: NL2SQL查询接口
系统SHALL通过route="/api/nl2sql"的agent配置提供NL2SQL查询能力。请求到达POST /api/nl2sql时，系统通过RouteRegistry路由到对应agent，将prompt作为user message发送给LLM，返回生成的SQL。

#### Scenario: 成功生成SQL
- **WHEN** 发送`POST /api/nl2sql`，body包含prompt（有效的NL2SQL user prompt）
- **THEN** 系统通过route路由到NL2SQL agent，将prompt作为user message发送给LLM，返回200，响应体包含生成的SQL和sessionId

#### Scenario: agent配置不存在
- **WHEN** route="/api/nl2sql"对应的agent配置在数据库中不存在
- **THEN** 返回404

#### Scenario: LLM生成失败
- **WHEN** LLM无法生成符合规范的SQL
- **THEN** 系统返回LLM的拒绝/询问消息，不返回SQL

### Requirement: NL2SQL流式接口
系统SHALL通过route="/api/nl2sql"的agent配置提供SSE流式接口GET /api/nl2sql/stream。

#### Scenario: 流式生成SQL
- **WHEN** 发送`GET /api/nl2sql/stream`，携带prompt参数
- **THEN** 系统以SSE事件流返回生成过程，格式与/api/chat/stream一致

### Requirement: User prompt透传
系统SHALL将API请求中的prompt字段作为LLM调用的user message，不做任何修改或拼接。

#### Scenario: 透传user prompt
- **WHEN** 请求中prompt为外部系统生成的完整user prompt
- **THEN** 系统直接将prompt作为user message发送给LLM，不修改内容

### Requirement: 多轮对话SQL调整
系统SHALL支持基于同一sessionId的多轮对话，对话历史由HarnessAgent的session memory在内存中维护。

#### Scenario: 修改已有SQL
- **WHEN** 用户在同一route+sessionId中发送新的prompt要求调整SQL
- **THEN** 系统基于该agent实例内存中的对话历史和新的prompt，生成调整后的SQL
