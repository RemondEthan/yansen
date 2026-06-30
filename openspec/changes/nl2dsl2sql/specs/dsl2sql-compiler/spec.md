## ADDED Requirements

### Requirement: DSL2SQL确定性编译
系统SHALL提供DSL2SQL编译器，输入QueryDsl + SemanticModel列表，输出StarRocks SQL。编译过程为确定性的，相同输入永远生成相同SQL。

#### Scenario: 单表聚合查询编译
- **WHEN** DSL包含metrics=[taxIncludedAmount(SUM)]、dimensions=[partyBName]，语义模型关联单表material_procurement_contract
- **THEN** 编译器生成：`SELECT mpc.partyBName AS partyBName, SUM(mpc.taxIncludedAmount) AS taxIncludedAmount FROM ods_db.material_procurement_contract mpc WHERE mpc.discarded = 0 GROUP BY mpc.partyBName ORDER BY mpc.partyBName`

#### Scenario: 多表JOIN编译
- **WHEN** DSL包含的metrics/dimensions跨两个语义模型，两个模型通过entity "contract"关联
- **THEN** 编译器推断JOIN路径，生成LEFT JOIN ON条件

#### Scenario: 时间范围过滤编译
- **WHEN** DSL filter包含dimension="signedDate" operator="between"
- **THEN** 编译器生成`WHERE mpc.signedDate BETWEEN ${START_SIGNED_DATE} AND ${END_SIGNED_DATE}`

### Requirement: 编译器自动添加逻辑删除过滤
系统SHALL在编译SQL时，自动为每个涉及的语义模型添加logicalDeleteField过滤条件。

#### Scenario: 自动添加逻辑删除
- **WHEN** 语义模型配置了logicalDeleteField name="discarded" filterValue="0"
- **THEN** 生成的SQL WHERE子句中包含`table.discarded = 0`

### Requirement: 编译器生成全限定表名和别名
系统SHALL在编译SQL时，所有表使用全限定名（库名.表名），每个表分配简短别名，字段使用别名引用。

#### Scenario: 全限定名和别名
- **WHEN** 编译涉及表ods_procurement_contract_gcbp.material_procurement_contract
- **THEN** SQL中使用`FROM ods_procurement_contract_gcbp.material_procurement_contract mpc`，字段引用`mpc.fieldName`

### Requirement: 编译器生成小驼峰别名和注释
系统SHALL在编译SQL时，每个SELECT字段生成小驼峰别名（下划线转驼峰），并添加`-- 注释`行内注释，注释使用dimension/measure的description。

#### Scenario: 别名和注释生成
- **WHEN** 编译dimension party_b_name（description="乙方名称"）
- **THEN** 生成`mpc.partyBName AS partyBName -- 乙方名称`

### Requirement: 编译器生成参数化占位符
系统SHALL在编译SQL时，filter的value若为参数占位符（${}格式），直接保留；时间范围参数使用${START_XXX}和${END_XXX}格式。

#### Scenario: 参数化占位符
- **WHEN** DSL filter value为["${START_SIGNED_DATE}", "${END_SIGNED_DATE}"]
- **THEN** 编译器生成`mpc.signedDate BETWEEN ${START_SIGNED_DATE} AND ${END_SIGNED_DATE}`

### Requirement: JOIN路径推断
系统SHALL根据DSL涉及的metrics/dimensions所在的语义模型，通过entity的primary/foreign关系推断JOIN路径。若无法推断JOIN路径，编译失败并报错。

#### Scenario: 推断两表JOIN
- **WHEN** DSL涉及语义模型A（entity contract: foreign）和语义模型B（entity contract: primary）
- **THEN** 编译器推断A LEFT JOIN B ON A.contract = B.contract

#### Scenario: JOIN路径不存在
- **WHEN** DSL涉及的两个语义模型无共享entity
- **THEN** 编译失败，返回错误"no join path between semantic models A and B"
