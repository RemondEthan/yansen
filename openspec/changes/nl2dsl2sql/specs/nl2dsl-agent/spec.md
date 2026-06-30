## ADDED Requirements

### Requirement: NL2DSL Agent
系统SHALL提供NL2DSL Agent（agentType="nl2dsl2sql"），将自然语言查询转换为DSL JSON。Agent的system prompt包含语义模型定义（entities/dimensions/measures列表），约束LLM输出为DSL JSON格式。

#### Scenario: 自然语言转DSL
- **WHEN** 用户输入"查询各乙方的含税合同金额，按签约日期范围筛选"，关联的语义模型包含measure taxIncludedAmount和dimension partyBName、signedDate
- **THEN** Agent输出DSL JSON：`{"metrics":[{"name":"taxIncludedAmount"}],"dimensions":["partyBName"],"filters":[{"dimension":"signedDate","operator":"between","value":["${START_SIGNED_DATE}","${END_SIGNED_DATE}"]}],"orderBy":[{"dimension":"partyBName","direction":"asc"}]}`

#### Scenario: 自然语言匹配不到metric
- **WHEN** 用户输入的查询意图无法匹配到语义模型中的任何measure
- **THEN** Agent返回询问消息，提示可用的metrics列表

#### Scenario: 自然语言匹配不到dimension
- **WHEN** 用户输入的分组需求无法匹配到语义模型中的任何dimension
- **THEN** Agent返回询问消息，提示可用的dimensions列表

### Requirement: NL2DSL Agent的system prompt包含语义模型
系统SHALL在NL2DSL Agent初始化时，将关联的语义模型定义注入system prompt，包含所有可用的metrics、dimensions、filters描述，供LLM做语义匹配。

#### Scenario: system prompt包含语义模型摘要
- **WHEN** NL2DSL Agent初始化，关联语义模型包含3个measures和5个dimensions
- **THEN** system prompt中包含这些measures和dimensions的名称、描述、类型列表

### Requirement: NL2DSL2SQL端到端API
系统SHALL提供`POST /api/nl2dsl2sql`接口，端到端完成NL→DSL→SQL：接收自然语言查询，先调用NL2DSL Agent生成DSL，再调用DSL2SQL编译器生成SQL，返回DSL和SQL。

#### Scenario: 端到端NL→DSL→SQL
- **WHEN** 发送`POST /api/nl2dsl2sql`，agentId关联nl2dsl2sql类型agent，userPrompt为自然语言查询
- **THEN** 系统返回DSL JSON和编译后的SQL

#### Scenario: 端到端SSE流式
- **WHEN** 发送`GET /api/nl2dsl2sql/stream`
- **THEN** 系统以SSE事件流返回，先推送DSL事件，再推送SQL事件

### Requirement: DSL2SQL独立API
系统SHALL提供`POST /api/dsl2sql`接口，接收DSL JSON和semanticModelId，调用编译器生成SQL。用于调试和手动修正DSL后重新编译。

#### Scenario: 手动DSL编译SQL
- **WHEN** 发送`POST /api/dsl2sql`，body包含DSL JSON和semanticModelId
- **THEN** 系统校验DSL合法性，编译为SQL，返回SQL
