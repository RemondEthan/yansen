# Repository Guidelines

## Architecture

Single-module Maven app (Java 21) on **agentscope-harness** (v2.0.0-RC4). Runs as a **Javalin HTTP server** (not a CLI agent).

**Config split:** `yansen.yml` holds infrastructure only (server ports/timeouts, database connection). All business config (models, agents, prompts, tools, skills, MCP) lives in **SQLite** (`ConfigStore`), initialized from `db/sqlite/schema.sql` + `db/init-data.sql`.

**Request flow:**

```
HTTP request
  → RouteRegistry (path → agentId)
  → AgentRegistry (lazy-load + in-memory cache)
  → AgentFactory (read SQLite records → build HarnessAgent)
  → YansenAgentImpl.chat() / chatStream()
```

**Startup (`AgentMain`):** load YAML → open ConfigStore → `AgentContext.bootstrap()` (SPI: ModelRegistry, ToolRegistry, SkillRegistry, McpRegistry) → register dynamic agent routes from DB → wire `ApiModule`.

### HTTP surface

| Category | Endpoints |
|----------|-----------|
| Health | `GET /api/health` — liveness + cached agents + route snapshot |
| Chat (per agent) | `POST {route}` — sync JSON; `GET {route}/stream` — SSE |
| Config CRUD | `POST/GET/PUT/DELETE /api/config/{model\|prompt\|tool\|skill\|mcp}` (+ `GET .../{id}`) |
| Agent config (read-only at runtime) | `GET /api/config/agent` (+ `GET .../{id}`) — see lifecycle note below |

Default seed registers two agents: `default` at `/api/chat` and `nl2sql` at `/api/nl2sql` (each with sync + SSE). **Agent set is closed at runtime**: `POST`/`PUT`/`DELETE` on `/api/config/agent[/{id}]` are not part of the supported surface, and direct `agent_config` writes are also unsupported. Adding, modifying, or deleting an agent requires editing `db/init-data.sql` (or the equivalent seed path) and restarting the service. The **only runtime-mutable resource tied to an agent is the model** — `/api/config/model` controls provider, modelName, baseUrl, apiKey, and timeouts/retries; the agent picks those up via lazy load on the next request.

**SSE:** `SseSession` handles heartbeat (`keepAliveIntervalSeconds`), event idle timeout, synchronized writes. Jetty connector idle timeout is set from `sseIdleTimeoutSeconds`.

**Agent cache:** `AgentRegistry` lazy-loads on first request; instances stay in memory until process shutdown or an internal race-resolution path (see agent lifecycle note above).

### Package map

```
com.glodon.mordor.yansen/
  AgentMain.java, AgentContext.java          – entry + wiring
  agent/                                     – YansenAgent, AgentFactory, YansenAgentImpl
  api/                                       – handlers, ConfigController, SseSession, ApiModule
  config/                                    – YansenConfig, YansenSettings (server + database only)
  config/store/                              – ConfigStore, records, ConfigValueResolver
  config/store/sqlite/                       – SqliteConfigStore, DAOs, HikariCP
  registry/                                  – RouteRegistry, AgentRegistry
  llm/, tool/, skill/, mcp/                  – SPI registries (discover / fromSkillRecords / fromMcpRecords)
src/main/resources/
  yansen.yml                                 – server + database placeholders
  db/sqlite/schema.sql, db/init-data.sql     – config DB bootstrap
  system-prompt.md, prompts/, skills/         – default + NL2SQL classpath prompts, bundled skills
```

Detailed design notes: `openspec/changes/nl2sql-agent/`.

## Build & Run

| Command | Description |
|---------|-------------|
| `mvn compile` | Compile |
| `mvn test` | Run tests (~130, JUnit 5) |
| `mvn package` | Build JAR + run tests |
| `mvn exec:java -Dexec.mainClass="com.glodon.mordor.yansen.AgentMain"` | Run server locally |

Requires `MINIMAX_API_KEY` (or model apiKey in DB) for LLM calls.

## Configuration

**YAML** (`yansen.yml`, override via `YANSEN_CONFIG_FILE`): `${ENV_VAR:default}` resolved at load time.

**SQLite** (`YANSEN_SQLITE_PATH`, default `store/yansen.db`): business config. No database-level `FOREIGN KEY` constraints — references are validated in application code (`ConfigController.validateAgentReferences` on agent CRUD, `ConfigReferenceException` on delete when still referenced). String values including `${ENV:default}` are stored **literally** in the database and returned as-is by CRUD APIs. Env substitution runs only when values are **consumed at runtime** via `ConfigValueResolver.resolveStored()`:

| Area | Resolved fields | When |
|------|-----------------|------|
| Model | `provider`, `modelName`, `baseUrl`, `apiKey` | `ModelConfigRecord.toModelSettings()` (agent build) |
| Agent | `modelId`, `workspace`, `route`, `agentType`; `toolIds` / `skillIds` / `mcpIds` as lookup keys | `AgentFactory`, `AgentRouteRegistrar` |
| Prompt | `sourceRef` (path); file/classpath content may contain placeholders too | `SqliteConfigStore.resolvePromptContent()` + agent build |
| Skill | `sourceRef` | `SkillRegistry.fromSkillRecords()` (bootstrap) and `toResolveId()` (per agent) |
| MCP | `config` (JSON string, including embedded placeholders) | `McpRegistry.fromMcpRecords()` (bootstrap) |

**Not resolved:** primary-key columns as stored (`agentId`, `modelId`, `toolId`, …) and CRUD list/get responses — only runtime lookups and wiring expand placeholders. Resolution is **single-pass** (if an env var value itself contains `${...}`, it is not expanded again).

| Env var | Purpose | Default |
|---------|---------|---------|
| `MINIMAX_API_KEY` | Default model API key (via DB placeholder) | *(empty)* |
| `YANSEN_PORT` | HTTP port | `8080` |
| `YANSEN_WORKSPACE` | Agent workspace root (DB: `${YANSEN_WORKSPACE:./agentscope}`) | `./agentscope` if unset |
| `YANSEN_SQLITE_PATH` | Config database file | `store/yansen.db` |
| `YANSEN_CONFIG_FILE` | External YAML override | *(none)* |
| `YANSEN_LOG_LEVEL` | SLF4J level for `com.glodon.mordor.yansen` | `info` |
| `YANSEN_KEEPALIVE_INTERVAL` | SSE heartbeat interval (s) | `10` |
| `YANSEN_SSE_IDLE_TIMEOUT` | Jetty connector idle timeout (s) | `300` |
| `YANSEN_SSE_EVENT_TIMEOUT` | SSE stream event gap timeout (s) | `120` |
| `YANSEN_CHAT_TIMEOUT` | Sync chat block timeout (s) | `300` |

**Skills:** built-in → `skill_config.sourceType=classpath`, files under `src/main/resources/skills/`. Custom → `sourceType=workspace`, files under `{workspace}/{sourceRef}/` (typically `$YANSEN_WORKSPACE/skills/`). Skill text is never stored in SQLite.

**Workspace:** per-agent field in `agent_config.workspace`. Set `YANSEN_WORKSPACE` to an **absolute path** in production to avoid dependence on process cwd.

MySQL ConfigStore is **planned** (`database.mysql` in yansen.yml + `db/mysql/schema.sql`) but **not implemented**; `database.type=mysql` fails at startup. Use SQLite (default).

## Coding Style

- Java 21, 4-space indent, no tabs.
- Packages by concern: `agent`, `api`, `config`, `config.store`, `registry`, `llm`, `tool`, `skill`, `mcp`.
- Class-level Javadoc with `@author` / `@date` / `@description`.

## Testing

JUnit 5 (`junit-jupiter`) + `javalin-testtools` for HTTP integration tests. Naming: `<Class>Test.java`. Run `mvn test`. New features should include tests where behavior is non-trivial.

## Commit Conventions

- Concise, imperative messages. Scope prefix when helpful: `llm: add ...`, `config: fix ...`.
- New features must include corresponding tests.

## Red Lines

These rules are absolute. Violation is a process error regardless of intent.

**No code or project state may be modified without explicit user approval.** "Explicit
approval" means an unambiguous yes to the specific edit, not silence, not a vague
"go ahead". When in doubt, stop and ask.

The following are **never** done without approval:

- Modifying any source file under `src/**` (Java, resources, YAML, Markdown, scripts,
  assembly, SPI files, tests)
- Modifying project-level files: `pom.xml`, `AGENTS.md`, `.gitignore`, `.editorconfig`
- Modifying editor/tool config: `.codex/**`, `.opencode/**`, `.claude/**`, `.cursor/**`
- Running build or packaging commands that write to `target/` (`mvn compile`,
  `mvn package`, `mvn install`, `mvn exec:java`)
- Running schema/DB migrations, or writing the config SQLite file
- Running any `git write` operation: `add`, `commit`, `push`, `merge`, `rebase`,
  `reset`, `checkout --`, `stash drop`, `tag`, `branch -D`, `clean`
- Running destructive filesystem operations: `rm`, `mv` overwriting,
  `chmod`/`chown` outside `cwd`
- Making outbound network calls beyond what read-only verification requires

The following are **always permitted** without further approval:

- Read-only inspection: `cat`, `nl`, `wc`, `rg`, `ls`, `find`, `git log`, `git status`,
  `git diff`, `git show`
- Drafting text in working memory for the user to review (proposals, design drafts,
  code snippets, diffs)

The following are **yellow zone** — ask first, even if not strictly "modifying code":

- Drafting an `apply_patch` block (writing the text, not running it)
- Creating a branch or worktree (`git checkout -b`, `git worktree add`)
- Running tests that have side effects (DB-touching integration tests, network calls)

If a request from the user or a step in a plan would violate a red line, the assistant
pauses and asks for explicit permission. There are no exceptions for "small", "obvious",
or "obviously correct" edits.
