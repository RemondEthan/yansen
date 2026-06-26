# SSE Stream Timeout Fix

## 问题现象

调用 `api/chat/stream` 时，上下文较大，远未到 900s 就报 `Timeout was reached`。

## 根因

agentscope 框架存在两层超时机制，语义完全不同：

**第一层：HTTP Transport `readTimeout`**
- 作用在 OkHttp/JDK HttpClient 的 socket 层
- 含义：从发起 HTTP 请求到读取完响应的**总耗时上限**
- 框架默认值：**5 分钟**（`HttpTransportConfig.DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5)`）
- 对 SSE 流：这是正确的超时旋钮

**第二层：`ExecutionConfig.timeout`**
- 作用在 Reactor 的 `Flux.timeout(Duration)` 操作符
- 含义：**两次 Flux 元素发射之间的最大间隔**，不是总耗时
- 框架默认值：**5 分钟**（`ExecutionConfig.MODEL_DEFAULTS.timeout = Duration.ofMinutes(5)`）
- 对 SSE 流：模型在 thinking 阶段可能长时间不发出任何事件，两次事件间隔超过 5 分钟就会触发超时——即使整个请求才刚开始

之前的修改把 `timeoutSeconds=900` 设到了 `ExecutionConfig.timeout` 上，等于加长了事件间隔容忍度，但**HTTP 层的 `readTimeout` 仍然是 5 分钟**，所以总耗时超过 5 分钟就被 HTTP 层杀掉了。同时 `Flux.timeout()` 在事件间隔场景下仍然可能误触发。

---

## 涉及文件（4 个）

### 1. `src/main/java/com/glodon/mordor/kiara/config/ModelSettings.java`

**改动**：新增 5 个超时相关字段

```java
// 之前
public record ModelSettings(
        String provider,
        String modelName,
        String baseUrl,
        String apiKey) {}

// 之后
public record ModelSettings(
        String provider,
        String modelName,
        String baseUrl,
        String apiKey,
        Integer timeoutSeconds,        // ExecutionConfig.timeout（已弃用，保留兼容）
        Integer maxRetries,            // ExecutionConfig.maxAttempts
        Integer connectTimeoutSeconds, // HttpTransportConfig.connectTimeout
        Integer readTimeoutSeconds,    // HttpTransportConfig.readTimeout ← 关键
        Integer writeTimeoutSeconds)   // HttpTransportConfig.writeTimeout
```

所有新增字段默认为 `null`，null 时回退到框架默认值，零侵入。

---

### 2. `src/main/java/com/glodon/mordor/kiara/llm/provider/OpenAiCompatibleProvider.java`

**改动**：构建 `OpenAIChatModel` 时，根据配置注入自定义 HTTP Transport 和 GenerateOptions

```java
// 之前
return OpenAIChatModel.builder()
        .modelName(settings.modelName())
        .apiKey(settings.apiKey())
        .baseUrl(settings.baseUrl())
        .stream(true)
        .build();

// 之后
var builder = OpenAIChatModel.builder()
        .modelName(settings.modelName())
        .apiKey(settings.apiKey())
        .baseUrl(settings.baseUrl())
        .stream(true);

// 1) 自定义 HTTP Transport（设置 connect/read/write 超时）
HttpTransportConfig transportConfig = buildTransportConfig(settings);
if (transportConfig != null) {
    builder.httpTransport(new OkHttpTransport(transportConfig));
}

// 2) GenerateOptions（仅设 maxRetries，不设 timeout）
GenerateOptions generateOptions = buildGenerateOptions(settings);
if (generateOptions != null) {
    builder.generateOptions(generateOptions);
}

return builder.build();
```

**`buildTransportConfig` 逻辑**：
```java
private static HttpTransportConfig buildTransportConfig(ModelSettings settings) {
    // 只有配置了至少一个 transport 超时才创建自定义 config
    boolean hasCustom = settings.connectTimeoutSeconds() != null
            || settings.readTimeoutSeconds() != null
            || settings.writeTimeoutSeconds() != null;
    if (!hasCustom) return null;

    var cfgBuilder = HttpTransportConfig.builder();
    if (settings.connectTimeoutSeconds() != null) {
        cfgBuilder.connectTimeout(Duration.ofSeconds(settings.connectTimeoutSeconds()));
    }
    if (settings.readTimeoutSeconds() != null) {
        cfgBuilder.readTimeout(Duration.ofSeconds(settings.readTimeoutSeconds()));
    }
    if (settings.writeTimeoutSeconds() != null) {
        cfgBuilder.writeTimeout(Duration.ofSeconds(settings.writeTimeoutSeconds()));
    }
    return cfgBuilder.build();
}
```

**`buildGenerateOptions` 逻辑**（关键修正）：
```java
private static GenerateOptions buildGenerateOptions(ModelSettings settings) {
    // 只把 maxRetries 传入 GenerateOptions
    // 不设 ExecutionConfig.timeout —— 它映射到 Reactor Flux.timeout()
    // 对 SSE 流，Flux.timeout() 检测的是事件间隔，不是总耗时
    // 模型 thinking 阶段可能长时间不发事件，导致误触发
    // 真正的总耗时上限由 HTTP 层 readTimeout 控制
    if (settings.maxRetries() == null) return null;

    return GenerateOptions.builder()
            .executionConfig(ExecutionConfig.builder()
                    .maxAttempts(settings.maxRetries())
                    .build())
            .build();
}
```

**为什么用 `OkHttpTransport` 而不是 `JdkHttpTransport`**：
- 框架默认 transport 是 `JdkHttpTransport`（`HttpTransportFactory.getDefault()` 创建）
- 但项目依赖中已有 OkHttp（`com.squareup.okhttp3:okhttp:5.3.2`），且 `OkHttpTransport` 对 SSE 流的 readTimeout 控制更稳定
- `OkHttpTransport(HttpTransportConfig)` 构造函数会根据 config 设置 `connectTimeout`、`readTimeout`、`writeTimeout`

---

### 3. `src/main/java/com/glodon/mordor/kiara/service/KiaraAgentService.java`

**改动**：通过 `modelExecutionConfig()` 传入 Agent 层的执行配置

```java
// 在 HarnessAgent.builder() 链式调用中新增：
ExecutionConfig execConfig = buildExecutionConfig(modelSettings);
if (execConfig != null) {
    builder.modelExecutionConfig(execConfig);
}
```

**`buildExecutionConfig` 逻辑**（关键修正）：
```java
private static ExecutionConfig buildExecutionConfig(ModelSettings settings) {
    // 只设 maxRetries，不设 timeout
    // 原因同 OpenAiCompatibleProvider：ExecutionConfig.timeout
    // 对 SSE 流会误判事件间隔超时
    if (settings.maxRetries() == null) return null;
    return ExecutionConfig.builder()
            .maxAttempts(settings.maxRetries())
            .build();
}
```

这确保 Agent 的 ReAct 循环中每次模型调用的重试策略也受配置控制。

---

### 4. `src/main/resources/kiara.yml`

**改动**：在 `models.default` 下新增超时配置

```yaml
models:
  default:
    provider: openai-compatible
    modelName: MiniMax-M3
    baseUrl: https://api.minimaxi.com/v1
    apiKey: "${MINIMAX_API_KEY:}"
    # Max retry attempts on retryable errors (timeout, rate-limit, IO).
    # Framework default is 3; set to 1 to disable retries.
    maxRetries: "${KIARA_MODEL_MAX_RETRIES:3}"
    # HTTP transport timeouts.
    # readTimeoutSeconds is the critical knob for large-context SSE streams —
    # it controls the OkHttp/JDK read timeout on the HTTP connection.
    # Framework defaults: connect=30s, read=5min(300s), write=30s
    connectTimeoutSeconds: "${KIARA_CONNECT_TIMEOUT:30}"
    readTimeoutSeconds: "${KIARA_READ_TIMEOUT:900}"
    writeTimeoutSeconds: "${KIARA_WRITE_TIMEOUT:30}"
```

关键变化：
- **移除了 `timeoutSeconds`**（之前设为 900），因为 `ExecutionConfig.timeout` 对 SSE 流有害
- `readTimeoutSeconds: 900`（15 分钟）— 这是唯一正确的总耗时上限
- 所有值支持环境变量覆盖，如 `KIARA_READ_TIMEOUT=1800`

---

## 超时控制全景

```
客户端请求
  │
  ▼
Javalin SSE handler（无超时限制，keepAlive 保活）
  │
  ▼
KiaraAgentService.chatStream()
  │
  ▼
HarnessAgent.streamEvents() → ReAct 循环
  │
  ├─ 每轮模型调用
  │   │
  │   ▼
  │  OpenAIChatModel.doStream()
  │   │
  │   ├─ ModelUtils.applyTimeoutAndRetry()
  │   │   ├─ Flux.timeout()  ← 不再设置，避免事件间隔误杀
  │   │   └─ retry(maxAttempts) ← maxRetries: 3
  │   │
  │   ▼
  │  OkHttpTransport.stream()  ← HTTP 层
  │   │
  │   └─ readTimeout = 900s  ← 真正的总耗时上限
  │
  └─ 工具调用（toolExecutionConfig，使用框架默认）
```
