## Context

当前NL2SQL方案（nl2sql-agent change）直接从自然语言生成SQL，存在准确率瓶颈：
- LLM需要同时理解业务语义（"乙方名称"→哪个表哪个字段）和SQL语法规范（全限定名、别名、JOIN、聚合、逻辑删除等）
- 两个关注点耦合，任一环节出错导致整体失败
- system prompt虽定义了严格规范，但LLM对复杂多表JOIN、指标口径、时间范围的推理仍易出错

dbt Semantic Layer提供了一个成熟的参考模型：
- **Semantic Model**：YAML抽象层，定义entities（关联键）、dimensions（分组维度）、measures（可聚合度量）
- **Metrics**：基于semantic model的指标定义，支持simple/cumulative/ratio/derived四种类型
- **MetricFlow**：将metric请求编译为dataflow查询计划，优化后生成引擎SQL
- 核心思想：**业务语义与SQL生成解耦**——用户在语义层操作，系统确定性编译为SQL

### NL→DSL→SQL vs NL→SQL 准确率分析

| 维度 | NL→SQL | NL→DSL→SQL |
|---|---|---|
| LLM任务 | 理解语义 + 生成SQL | 仅理解语义，映射到DSL |
| 输出空间 | 无限（任意SQL字符串） | 有限（语义模型定义的metrics×dimensions×filters组合） |
| JOIN推理 | LLM自行推断（易错） | 编译器基于entity关系确定性生成 |
| 聚合函数 | LLM选择（可能选错） | measure定义中已指定agg类型 |
| 逻辑删除 | LLM推断（可能遗漏） | 编译器基于模型配置自动添加 |
| 别名/注释 | LLM生成（可能不规范） | 编译器确定性生成 |
| 可调试性 | 黑盒，出错难定位 | DSL可审查，编译过程可追溯 |
| 失败模式 | SQL语法错误/语义错误 | 仅DSL匹配错误（语义层问题） |

**结论**：NL→DSL→SQL将LLM的不确定性限制在语义匹配环节，SQL生成变为确定性编译，整体准确率应显著提升。

## Goals / Non-Goals

**Goals:**
- 定义语义模型（借鉴dbt semantic model），包含entities、dimensions、measures、time_dimensions
- 定义查询DSL（JSON schema），结构化表达查询意图：metrics、dimensions、filters、time_range
- 实现NL2DSL Agent：自然语言→DSL，LLM在语义模型空间内匹配
- 实现DSL2SQL编译器：DSL→StarRocks SQL，确定性编译（JOIN路径、聚合、逻辑删除、参数化）
- 两种路径并存（NL2SQL和NL2DSL2SQL），按agentType选择，支持准确率对比

**Non-Goals:**
- 不做MetricFlow的完整dataflow查询计划优化（当前阶段直接编译SQL即可）
- 不做dbt的derived/ratio/conversion metric类型（先支持simple和cumulative）
- 不做DSL的可视化编辑器
- 不做语义模型的自动从数据库推断（人工配置）
- 不替换现有NL2SQL路径（两者并存）

## Decisions

### 1. 语义模型借鉴dbt，但简化适配StarRocks场景

**选择**：定义SemanticModel，包含：
- `entities`：关联键（primary/foreign/unique），用于推断JOIN路径
- `dimensions`：分组维度（含time_dimensions时间维度），映射到表字段
- `measures`：可聚合度量，指定agg类型（SUM/AVG/COUNT/MAX/MIN）和字段

**与dbt的差异**：
- 不区分semantic_model和metrics两层——measures直接定义在semantic_model中（简化，dbt的metrics是独立定义引用semantic_model的measures）
- entities直接使用物理字段名（dbt支持expr表达式，当前不需要）
- 时间维度标注`timeGrain`（日/月/季/年），用于时间范围编译

**理由**：dbt的完整spec过于复杂（ratio/derived/conversion metrics、entity expr等），当前场景用不到。简化版保留核心价值（语义解耦+确定性编译），降低实现成本。

### 2. DSL为JSON格式，LLM输出结构化JSON而非自由文本

**选择**：DSL定义为JSON schema，NL2DSL Agent的LLM输出为结构化JSON：

```json
{
  "metrics": [{"name": "taxIncludedAmount", "agg": "sum"}],
  "dimensions": ["partyBName", "contractCategoryName"],
  "filters": [
    {"dimension": "discarded", "operator": "eq", "value": 0},
    {"dimension": "signedDate", "operator": "between", "value": ["${START_SIGNED_DATE}", "${END_SIGNED_DATE}"]}
  ],
  "orderBy": [{"dimension": "partyBName", "direction": "asc"}],
  "limit": null
}
```

**替代方案**：DSL为自定义文本语法——需要额外解析器，且LLM生成自由文本的格式一致性不如JSON。

**理由**：JSON schema可以注入LLM的system prompt作为输出约束（structured output / function calling），LLM生成JSON的准确率远高于生成SQL。JSON也便于校验和调试。

### 3. DSL2SQL编译器为确定性Java代码，非LLM

**选择**：DSL2SQL编译器为纯Java实现，输入DSL JSON + SemanticModel，输出StarRocks SQL。编译逻辑包括：
- 根据metrics和dimensions涉及的semantic_model，推断JOIN路径（基于entities的primary/foreign关系）
- 根据measures的agg类型生成聚合函数
- 自动添加逻辑删除过滤（基于semantic_model配置的logicalDeleteField）
- 生成全限定表名、小驼峰别名、字段注释
- 参数化占位符（${}格式）

**替代方案**：DSL2SQL也用LLM——引入新的不确定性，违背分层解耦的初衷。

**理由**：编译器是确定性的，同样的DSL+SemanticModel永远生成同样的SQL。这是NL→DSL→SQL方案的核心价值——将不确定性限制在NL→DSL环节。

### 4. 两种路径并存，按agentType区分

**选择**：在agent_config表中，agentType支持`nl2sql`和`nl2dsl2sql`两种值：
- `nl2sql`：现有路径，NL→SQL直出
- `nl2dsl2sql`：新路径，NL→DSL→SQL两步出

NL2DSL2SQLAgent内部组合NL2DSL Agent + DSL2SQL编译器。

**理由**：两种路径并存便于准确率对比。生产环境稳定后可选择默认路径，或根据查询复杂度自动选择（简单查询走NL2SQL，复杂查询走NL2DSL2SQL）。

### 5. 语义模型存储在配置数据库，提供CRUD API

**选择**：语义模型存储在SQLite配置数据库中（与nl2sql-agent change的ConfigDatabase共用），表包括semantic_model、semantic_entity、semantic_dimension、semantic_measure。提供CRUD API。

**理由**：与model/agent配置统一管理，复用ConfigDatabase基础设施。

## Risks / Trade-offs

- [语义模型配置成本] → 需要人工定义每个表的semantic model，初始投入较高；但一次配置长期受益，且比调试错误SQL的成本低
- [NL2DSL的LLM仍可能匹配错误metric/dimension] → DSL可审查可修正，比SQL更容易人工校验；且语义空间有限，错误范围可控
- [编译器无法覆盖所有SQL场景] → 复杂场景（窗口函数、CTE、子查询）暂不支持，可降级到NL2SQL直出路径
- [两种路径增加维护成本] → 两者共享底层agent基础设施，增量维护成本可控；对比期结束后可选择主路径
- [dbt语义模型简化可能限制未来扩展] → 当前简化版覆盖核心场景，后续可按需引入ratio/derived metrics等高级特性
