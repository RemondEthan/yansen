## ADDED Requirements

### Requirement: 查询DSL JSON Schema定义
系统SHALL定义查询DSL的JSON schema，包含：metrics（数组，每项含name和可选agg覆盖）、dimensions（数组，维度名）、filters（数组，每项含dimension/operator/value）、orderBy（数组）、limit（可选整数）。

#### Scenario: DSL JSON结构
- **WHEN** 定义DSL schema
- **THEN** 合法DSL示例：`{"metrics":[{"name":"taxIncludedAmount"}],"dimensions":["partyBName"],"filters":[{"dimension":"signedDate","operator":"between","value":["${START_SIGNED_DATE}","${END_SIGNED_DATE}"]}],"orderBy":[{"dimension":"partyBName","direction":"asc"}]}`

### Requirement: DSL校验
系统SHALL校验DSL的合法性：metrics和dimensions引用的名称在语义模型中存在、filter的operator合法（eq/ne/gt/lt/gte/lte/in/between/like）、orderBy的direction合法（asc/desc）。

#### Scenario: DSL引用不存在的metric
- **WHEN** DSL中metrics包含name="unknownMetric"，语义模型中无该measure
- **THEN** 校验失败，返回错误提示"metric 'unknownMetric' not found in semantic models"

#### Scenario: DSL引用不存在的dimension
- **WHEN** DSL中dimensions包含"unknownDim"，语义模型中无该dimension
- **THEN** 校验失败，返回错误提示"dimension 'unknownDim' not found in semantic models"

#### Scenario: filter operator不合法
- **WHEN** DSL中filter operator="invalid_op"
- **THEN** 校验失败，返回错误提示"unsupported filter operator 'invalid_op'"

### Requirement: DSL序列化与反序列化
系统SHALL支持DSL的JSON序列化/反序列化，用于LLM输出解析和API传输。

#### Scenario: 从JSON解析DSL
- **WHEN** 输入合法的DSL JSON字符串
- **THEN** 系统解析为QueryDsl Java record

#### Scenario: DSL转JSON
- **WHEN** 输入QueryDsl Java record
- **THEN** 系统序列化为JSON字符串
