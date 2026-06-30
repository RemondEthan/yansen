## ADDED Requirements

### Requirement: 语义模型定义
系统SHALL支持定义SemanticModel，包含：modelId、name、description、refTable（全限定表名）、entities、dimensions、measures。存储在配置数据库semantic_model表中。

#### Scenario: 创建语义模型
- **WHEN** 通过API创建语义模型，包含entities、dimensions、measures定义
- **THEN** 系统校验refTable引用的表存在、entities/dimensions/measures引用的字段存在，存入数据库

#### Scenario: 语义模型引用不存在的表
- **WHEN** 创建语义模型，refTable引用了数据库中不存在的表
- **THEN** 返回校验错误

### Requirement: Entity定义
系统SHALL支持在语义模型中定义entities，每个entity包含：name、type（primary/foreign/unique）、expr（字段名或SQL表达式）。primary entity用于标识表的唯一行，foreign entity用于推断跨表JOIN路径。

#### Scenario: 定义primary entity
- **WHEN** 语义模型定义entity name="contract" type="primary"
- **THEN** 该entity标识此表的唯一行，用于JOIN时作为驱动表

#### Scenario: 定义foreign entity推断JOIN
- **WHEN** 语义模型A定义entity name="contract" type="foreign"，语义模型B定义entity name="contract" type="primary"
- **THEN** 编译器推断A JOIN B ON A.contract = B.contract

### Requirement: Dimension定义
系统SHALL支持在语义模型中定义dimensions，每个dimension包含：name、type（categorical/time）、expr（字段名）、description、timeGrain（仅time类型，支持day/month/quarter/year）。

#### Scenario: 定义分类维度
- **WHEN** 定义dimension name="partyBName" type="categorical" expr="party_b_name"
- **THEN** 该维度可用于GROUP BY和WHERE过滤

#### Scenario: 定义时间维度
- **WHEN** 定义dimension name="signedDate" type="time" expr="signed_date" timeGrain="day"
- **THEN** 该维度可用于时间范围过滤，编译器生成BETWEEN条件

### Requirement: Measure定义
系统SHALL支持在语义模型中定义measures，每个measure包含：name、agg（SUM/AVG/COUNT/COUNT_DISTINCT/MAX/MIN）、expr（字段名）、description、aggTimeDimension（时间维度名，用于累计计算）。

#### Scenario: 定义求和度量
- **WHEN** 定义measure name="taxIncludedAmount" agg="SUM" expr="tax_included_amount"
- **THEN** 编译器生成SUM(table.taxIncludedAmount)

#### Scenario: 定义累计度量
- **WHEN** 定义measure name="ytdTaxIncludedAmount" agg="SUM" expr="tax_included_amount" aggTimeDimension="signedDate"
- **THEN** 编译器生成年累计时间范围条件

### Requirement: 逻辑删除字段配置
系统SHALL支持在语义模型中配置logicalDeleteField，包含fieldName和filterValue（如discarded=0）。编译器自动在WHERE中添加该过滤条件。

#### Scenario: 配置逻辑删除字段
- **WHEN** 语义模型配置logicalDeleteField name="discarded" filterValue="0"
- **THEN** 编译器生成的SQL中自动包含`WHERE table.discarded = 0`

### Requirement: 语义模型CRUD API
系统SHALL提供语义模型的CRUD API：`POST /api/config/semantic-model`（创建）、`GET /api/config/semantic-model`（列表）、`GET /api/config/semantic-model/{id}`（查询）、`PUT /api/config/semantic-model/{id}`（更新）、`DELETE /api/config/semantic-model/{id}`（删除）。

#### Scenario: 创建语义模型API
- **WHEN** 发送`POST /api/config/semantic-model`，body包含完整的语义模型定义
- **THEN** 校验通过后存入数据库，返回201

#### Scenario: 删除语义模型
- **WHEN** 发送`DELETE /api/config/semantic-model/{id}`
- **THEN** 删除语义模型及其关联的entities/dimensions/measures，返回204
