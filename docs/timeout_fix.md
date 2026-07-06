# SSE / Chat Timeout (Yansen)

> **Status:** Implemented in Yansen (2026-07). Operational reference — not a pending change list.
> Migrated from Kiara; all paths use `com.glodon.mordor.yansen.*`.

## 问题现象

调用 `{route}/stream`（默认 `GET /api/chat/stream`）时，上下文较大，远未到 900s 就报 `Timeout was reached`。

## 根因

agentscope 框架存在两层超时机制，语义完全不同：

**第一层：HTTP Transport `readTimeout`**
- 作用在 OkHttp/JDK HttpClient 的 socket 层
- 含义：从发起 HTTP 请求到读取完响应的**总耗时上限**
- 框架默认值：**5 分钟**（`HttpTransportConfig.DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5)`）
- 对 SSE 流：这是控制 LLM 上游总耗时的正确旋钮

**第二层：`ExecutionConfig.timeout`**
- 作用在 Reactor 的 `Flux.timeout(Duration)` 操作符
- 含义：**两次 Flux 元素发射之间的最大间隔**，不是总耗时
- 框架默认值：**5 分钟**（`ExecutionConfig.MODEL_DEFAULTS.timeout = Duration.ofMinutes(5)`）
- 对 SSE 流：模型在 thinking 阶段可能长时间不发出任何事件，两次事件间隔超过 5 分钟就会触发超时——即使整个请求才刚开始

错误做法是把 `timeoutSeconds=900` 设到 `ExecutionConfig.timeout` 上：加长了事件间隔容忍度，但 **HTTP 层的 `readTimeout` 仍然是 5 分钟**，总耗时超过 5 分钟仍会被 HTTP 层杀掉；`Flux.timeout()` 在事件间隔场景下仍可能误触发。

**正确做法：** 用 HTTP `readTimeoutSeconds` 控制上游总耗时；`ExecutionConfig` 只设 `maxRetries`，**不设 `timeout`**。

Yansen 还在应用层增加了第三组旋钮（Jetty idle、SSE heartbeat、Flux 事件间隔），见下文 §5–§6。

---

## 配置一览

| 旋钮 | 存储位置 | 默认值 | 控制什么 |
|------|----------|--------|----------|
| `readTimeoutSeconds` | SQLite `model_config` | **900** | LLM HTTP 上游总耗时上限（关键） |
| `connectTimeoutSeconds` | SQLite `model_config` | 30 | HTTP 连接建立超时 |
| `writeTimeoutSeconds` | SQLite `model_config` | 30 | HTTP 写超时 |
| `maxRetries` | SQLite `model_config` | 3 | 可重试错误的 maxAttempts（不设 ExecutionConfig.timeout） |
| `chatTimeoutSeconds` | `yansen.yml` → `server` | 300 | 同步 `POST {route}` 的 block 总超时 |
| `keepAliveIntervalSeconds` | `yansen.yml` → `server` | 10 | SSE 心跳间隔（`:keepalive` comment） |
| `sseIdleTimeoutSeconds` | `yansen.yml` → `server` | 300 | Jetty connector idle timeout |
| `sseEventTimeoutSeconds` | `yansen.yml` → `server` | 120 | SSE Flux 两次事件间最大间隔 |

Model 字段通过 `PUT /api/config/model/{id}` 或 `init-data.sql` 修改；server 字段通过 `yansen.yml` / `YANSEN_*` 环境变量修改。

---

## 当前涉及文件

### 1. `src/main/java/com/glodon/mordor/yansen/config/ModelSettings.java`

SPI 契约类型，含超时字段（均为 `Integer`，`null` 时回退框架默认）：

```java
public record ModelSettings(
        String provider,
        String modelName,
        String baseUrl,
        String apiKey,
        @Deprecated Integer timeoutSeconds,   // 已弃用，勿用于 SSE
        Integer maxRetries,
        Integer connectTimeoutSeconds,        // HttpTransportConfig.connectTimeout
        Integer readTimeoutSeconds,           // HttpTransportConfig.readTimeout ← 关键
        Integer writeTimeoutSeconds)          // HttpTransportConfig.writeTimeout
```

持久化类型 `ModelConfigRecord` 经 `toModelSettings()` 转换后传入 `ModelRegistry.create()`。

---

### 2. `src/main/java/com/glodon/mordor/yansen/llm/provider/OpenAiCompatibleProvider.java`

构建 `OpenAIChatModel` 时注入自定义 HTTP Transport 和 GenerateOptions：

- **`buildTransportConfig`** — 配置了 connect/read/write 任一字段时，使用 `OkHttpTransport`（SSE readTimeout 更稳定）
- **`buildGenerateOptions`** — 仅设 `maxAttempts`（来自 `maxRetries`），**不设 `ExecutionConfig.timeout`**

```java
// GenerateOptions：只传 maxRetries，不传 timeout
return GenerateOptions.builder()
        .executionConfig(ExecutionConfig.builder()
                .maxAttempts(settings.maxRetries())
                .build())
        .build();
```

---

### 3. `src/main/java/com/glodon/mordor/yansen/agent/AgentFactory.java`

构建 `HarnessAgent` 时传入 Agent 层执行配置（原 Kiara 项目中 `KiaraAgentService` 的职责）：

```java
ExecutionConfig execConfig = buildExecutionConfig(modelSettings);
if (execConfig != null) {
    builder.modelExecutionConfig(execConfig);
}
```

`buildExecutionConfig` 同样**只设 `maxRetries`，不设 `timeout`**。

---

### 4. SQLite `model_config`（默认 seed：`db/init-data.sql`）

业务 model 配置在 SQLite，不在 `yansen.yml`：

```sql
INSERT INTO model_config (..., maxRetries, connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds)
VALUES ('default', ..., 3, 30, 900, 30);
```

运行时可通过 CRUD 调整：

```bash
curl -X PUT http://localhost:8080/api/config/model/default \
  -H 'Content-Type: application/json' \
  -d '{"modelId":"default","provider":"openai-compatible","modelName":"MiniMax-M3",
       "baseUrl":"https://api.minimaxi.com/v1","apiKey":"${MINIMAX_API_KEY:}",
       "maxRetries":3,"connectTimeoutSeconds":30,"readTimeoutSeconds":900,"writeTimeoutSeconds":30}'
```

`apiKey` 等 `${ENV:default}` 占位符存库为字面量，在 agent 实例化时解析。

---

### 5. `yansen.yml` → `server`（SSE / 同步 chat）

```yaml
server:
  keepAliveIntervalSeconds: "${YANSEN_KEEPALIVE_INTERVAL:10}"
  sseIdleTimeoutSeconds: "${YANSEN_SSE_IDLE_TIMEOUT:300}"
  sseEventTimeoutSeconds: "${YANSEN_SSE_EVENT_TIMEOUT:120}"
  chatTimeoutSeconds: "${YANSEN_CHAT_TIMEOUT:300}"
```

- **`keepAliveIntervalSeconds`** — `SseSession` 定时发送 SSE comment，防止代理/Jetty idle 断连；须 **小于** `sseIdleTimeoutSeconds`（`ServerSettings` 会自动校正）
- **`sseIdleTimeoutSeconds`** — `AgentMain` 设置 Jetty connector idle timeout
- **`sseEventTimeoutSeconds`** — `SseSession` 对上游 `Flux<AgentEvent>` 的 `.timeout()`（事件间隔）
- **`chatTimeoutSeconds`** — `YansenAgentImpl.chat()` 的 `blockOptional` 总超时（仅同步接口）

部署在反向代理后时，代理 read-idle timeout 应 **大于** `keepAliveIntervalSeconds`。

---

### 6. SSE / Chat 应用层

| 类 | 职责 |
|----|------|
| `api/AgentStreamHandler.java` | 解析 route → agent → 创建 `SseSession` |
| `api/SseSession.java` | heartbeat、Flux 订阅、`sseEventTimeoutSeconds` |
| `agent/YansenAgentImpl.java` | `chatStream()` 透传 Flux；`chat()` 受 `chatTimeoutSeconds` 限制 |
| `AgentMain.java` | Jetty `sseIdleTimeoutSeconds` |

---

## 超时控制全景

```
客户端请求 GET {route}/stream
  │
  ▼
Javalin SSE handler
  │  Jetty connector idleTimeout = sseIdleTimeoutSeconds (AgentMain)
  │  SseSession heartbeat every keepAliveIntervalSeconds
  │
  ▼
AgentStreamHandler → AgentRegistry → YansenAgentImpl.chatStream()
  │
  ▼
HarnessAgent.streamEvents() → ReAct 循环
  │
  ├─ 每轮模型调用
  │   │
  │   ▼
  │  OpenAIChatModel.doStream()  (OpenAiCompatibleProvider)
  │   │
  │   ├─ ModelUtils.applyTimeoutAndRetry()
  │   │   ├─ Flux.timeout()  ← 不设 ExecutionConfig.timeout，避免事件间隔误杀
  │   │   └─ retry(maxAttempts) ← model_config.maxRetries
  │   │
  │   ▼
  │  OkHttpTransport.stream()
  │   └─ readTimeout = model_config.readTimeoutSeconds (默认 900s) ← 上游总耗时上限
  │
  └─ 工具调用（toolExecutionConfig，框架默认）
  │
  ▼
SseSession 订阅 Flux
  └─ Flux.timeout(sseEventTimeoutSeconds)  ← 应用层事件间隔（默认 120s）
```

同步 `POST {route}` 路径在 `YansenAgentImpl.chat()` 处以 `chatTimeoutSeconds`（默认 300s）block 等待完整响应。

---

## 调参建议

| 症状 | 优先调整 |
|------|----------|
| 大上下文 SSE 远不到 15 分钟就断 | 提高 `model_config.readTimeoutSeconds`（默认已 900） |
| 代理/负载均衡提前断 SSE | 提高代理 idle timeout；确认 `keepAliveIntervalSeconds` < 代理 timeout |
| Jetty 空闲断连 | 提高 `sseIdleTimeoutSeconds` |
| 模型长时间 thinking 无事件 | 提高 `sseEventTimeoutSeconds`（注意与 readTimeout 是不同层） |
| 同步 chat 超时 | 提高 `server.chatTimeoutSeconds` |

**不要** 恢复使用已弃用的 `ModelSettings.timeoutSeconds` / `ExecutionConfig.timeout` 来解决 SSE 总耗时问题。
