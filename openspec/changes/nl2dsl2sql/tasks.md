## 1. 语义模型数据模型与存储

- [ ] 1.1 定义SemanticModel record（modelId、name、description、refTable、logicalDeleteField）
- [ ] 1.2 定义SemanticEntity record（modelId、name、type[primary/foreign/unique]、expr）
- [ ] 1.3 定义SemanticDimension record（modelId、name、type[categorical/time]、expr、description、timeGrain）
- [ ] 1.4 定义SemanticMeasure record（modelId、name、agg[SUM/AVG/COUNT/COUNT_DISTINCT/MAX/MIN]、expr、description、aggTimeDimension）
- [ ] 1.5 ConfigDatabase新增建表：semantic_model、semantic_entity、semantic_dimension、semantic_measure（SQLiteDialect新增DDL）
- [ ] 1.6 实现SemanticModelDao：CRUD操作（含级联删除entities/dimensions/measures）
- [ ] 1.7 实现SemanticModel校验器：refTable存在性、字段引用存在性

## 2. 语义模型CRUD API

- [ ] 2.1 实现SemanticModelRequest/Response DTO（含嵌套的entities/dimensions/measures）
- [ ] 2.2 实现ConfigController扩展：`POST /api/config/semantic-model`创建端点
- [ ] 2.3 实现ConfigController扩展：`GET /api/config/semantic-model`列表端点
- [ ] 2.4 实现ConfigController扩展：`GET /api/config/semantic-model/{id}`查询端点
- [ ] 2.5 实现ConfigController扩展：`PUT /api/config/semantic-model/{id}`更新端点
- [ ] 2.6 实现ConfigController扩展：`DELETE /api/config/semantic-model/{id}`删除端点（级联删除）
- [ ] 2.7 在ApiModule中注册语义模型路由

## 3. 查询DSL定义与校验

- [ ] 3.1 定义QueryDsl record（metrics、dimensions、filters、orderBy、limit）
- [ ] 3.2 定义DslMetric record（name、可选agg覆盖）
- [ ] 3.3 定义DslFilter record（dimension、operator、value）
- [ ] 3.4 定义DslOrderBy record（dimension、direction）
- [ ] 3.5 定义DSL JSON schema常量（用于注入LLM system prompt）
- [ ] 3.6 实现DslValidator：校验metrics/dimensions引用存在性、filter operator合法性
- [ ] 3.7 实现DSL JSON序列化/反序列化（Jackson ObjectMapper）

## 4. DSL2SQL编译器

- [ ] 4.1 实现Dsl2SqlCompiler类：输入QueryDsl + List<SemanticModel>，输出String SQL
- [ ] 4.2 实现JOIN路径推断：根据DSL涉及的语义模型，通过entity primary/foreign关系推断JOIN路径
- [ ] 4.3 实现SELECT子句生成：metrics→聚合函数+小驼峰别名+注释，dimensions→字段+别名+注释
- [ ] 4.4 实现FROM/JOIN子句生成：全限定表名+别名+ON条件
- [ ] 4.5 实现WHERE子句生成：逻辑删除过滤+DSL filters编译+时间范围参数化
- [ ] 4.6 实现GROUP BY子句生成：所有dimensions字段
- [ ] 4.7 实现ORDER BY子句生成：DSL orderBy编译
- [ ] 4.8 实现别名转换工具：下划线→小驼峰（复用meta/system_prompt.md中的规则）

## 5. NL2DSL Agent

- [ ] 5.1 实现NL2DSL2SQLAgent类（YansenAgent接口）：组合NL2DSL步骤+DSL2SQL编译步骤
- [ ] 5.2 实现语义模型→system prompt摘要生成：将measures/dimensions列表格式化为LLM可理解的文本
- [ ] 5.3 NL2DSL2SQLAgent.chat()流程：LLM生成DSL JSON→解析→校验→编译SQL→返回
- [ ] 5.4 NL2DSL2SQLAgent.chatStream()流程：SSE流式返回DSL事件+SQL事件
- [ ] 5.5 AgentFactory扩展：agentType="nl2dsl2sql"时实例化NL2DSL2SQLAgent

## 6. API端点

- [ ] 6.1 实现Nl2dsl2sqlRequest DTO（agentId、userPrompt、userId、sessionId）
- [ ] 6.2 实现Nl2dsl2sqlResponse DTO（dsl、sql、sessionId、message）
- [ ] 6.3 实现Nl2dsl2sqlController：`POST /api/nl2dsl2sql`同步端点
- [ ] 6.4 实现Nl2dsl2sqlController：`GET /api/nl2dsl2sql/stream` SSE流式端点
- [ ] 6.5 实现Dsl2SqlRequest DTO（dsl、semanticModelId）
- [ ] 6.6 实现Dsl2SqlController：`POST /api/dsl2sql`独立编译端点（用于调试）
- [ ] 6.7 在ApiModule中注册NL2DSL2SQL和DSL2SQL路由

## 7. 测试

- [ ] 7.1 编写SemanticModel CRUD单元测试
- [ ] 7.2 编写DslValidator单元测试（合法DSL、非法metric/dimension/operator）
- [ ] 7.3 编写Dsl2SqlCompiler单元测试（单表聚合、多表JOIN、时间范围、逻辑删除、别名注释、参数化）
- [ ] 7.4 编写JOIN路径推断单元测试（两表JOIN、三表JOIN、无JOIN路径报错）
- [ ] 7.5 编写NL2DSL2SQLAgent集成测试（端到端NL→DSL→SQL）
- [ ] 7.6 编写Nl2dsl2sqlController API测试
- [ ] 7.7 编写Dsl2SqlController API测试（手动DSL编译）
- [ ] 7.8 准确率对比测试：同一组自然语言查询，分别走NL2SQL和NL2DSL2SQL路径，对比SQL正确率
