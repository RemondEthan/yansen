## Why

NL2SQL直接从自然语言生成SQL，LLM需要同时理解业务语义和SQL语法规范，准确率难以达标。引入中间DSL层（NL→DSL→SQL）将问题分解为两步：第一步NL→DSL只需理解业务语义映射到结构化查询意图，第二步DSL→SQL是确定性的编译转换，准确率可控。借鉴dbt Semantic Layer的语义模型（semantic model + metrics + dimensions + entities），定义业务语义层，让LLM在更小、更确定的语义空间内操作，显著提升生成准确率。

## What Changes

- 新增语义模型（SemanticModel）定义，借鉴dbt：包含entities（主键/外键关联）、dimensions（维度属性）、measures（可聚合度量）、dimensions时间维度
- 新增DSL定义：结构化的查询意图表达，包含metric选择、dimension分组、filter条件、时间范围，是自然语言到SQL的中间表示
- 新增NL2DSL Agent：将自然语言查询转换为DSL（结构化JSON），LLM只需在语义模型空间内做匹配，无需理解SQL语法
- 新增DSL2SQL编译器：将DSL确定性编译为StarRocks SQL，基于语义模型生成全限定表名、JOIN路径、聚合函数、逻辑删除过滤等
- 保留现有NL2SQL路径（NL→SQL直出），两种路径并存，可按agentId选择，便于准确率对比

## Capabilities

### New Capabilities
- `semantic-model`: 语义模型定义与存储——借鉴dbt semantic model，定义entities/dimensions/measures/time_dimensions，存储在配置数据库中，提供CRUD API
- `query-dsl`: 查询DSL定义与校验——结构化查询意图表达（metrics/dimensions/filters/time_range），JSON schema定义，校验DSL合法性
- `nl2dsl-agent`: NL2DSL Agent——将自然语言转换为DSL，LLM在语义模型空间内匹配，输出结构化JSON
- `dsl2sql-compiler`: DSL2SQL编译器——将DSL确定性编译为StarRocks SQL，基于语义模型解析JOIN路径、聚合、过滤、参数化

### Modified Capabilities
- `nl2sql-agent`（来自nl2sql-agent change）：扩展支持两种路径——NL2SQL直出和NL2DSL2SQL两步出，按agent配置的agentType区分

## Impact

- 新增Java包：`com.glodon.mordor.yansen.semantic`（语义模型）、`com.glodon.mordor.yansen.dsl`（DSL定义与校验）、`com.glodon.mordor.yansen.compiler`（DSL2SQL编译器）
- 新增数据库表：semantic_model、semantic_entity、semantic_dimension、semantic_measure
- 新增API端点：语义模型CRUD、`POST /api/nl2dsl`（NL→DSL）、`POST /api/dsl2sql`（DSL→SQL）、`POST /api/nl2dsl2sql`（NL→DSL→SQL端到端）
- 现有NL2SQL路径不受影响，两种路径并存
