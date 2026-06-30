# Repository Guidelines

## Architecture

Single-module Maven app (Java 21) built on **agentscope-harness** (v2.0.0-RC4). Runs as a **Javalin HTTP server** (not a CLI agent).

- `POST /api/chat` — synchronous chat (JSON body → JSON response)
- `GET /api/chat/stream` — SSE streaming chat (query params → event stream)

```
src/main/java/com/glodon/mordor/yansen/
  AgentMain.java              – Javalin server entry point, route definitions
  config/YansenConfig.java    – YAML config with ${ENV_VAR:default} placeholder resolution
  llm/MinimaxModel.java       – factory for MiniMax-M3 model (OpenAI-compatible via agentscope)
  model/ChatRequest.java      – chat request record DTO
  service/YansenAgentService.java – HarnessAgent lifecycle, chat/stream operations
src/main/resources/
  yansen.yml                  – default config (env var placeholders)
  system-prompt.md            – default system prompt (classpath fallback)
agentscope/                   – agent workspace runtime data (not source)
```

## Build & Run

| Command | Description |
|---|---|
| `mvn compile` | Compile |
| `mvn package` | Build JAR (runs tests) |

To run: `mvn exec:java -Dexec.mainClass="com.glodon.mordor.yansen.AgentMain"` (requires `exec-maven-plugin` in pom.xml — not currently configured).

## Configuration

Config loaded from classpath `yansen.yml`, overridden by external file via `YANSEN_CONFIG_FILE` env var. Values support `${ENV_VAR:default}` placeholders.

| Env var | Purpose | Default |
|---|---|---|
| `MINIMAX_API_KEY` | MiniMax API key | *(empty — required)* |
| `YANSEN_PORT` | HTTP server port | `8080` |
| `YANSEN_WORKSPACE` | Agent workspace path | *(empty)* |
| `YANSEN_SYSTEM_PROMPT` | External system prompt file path | *(classpath fallback)* |
| `YANSEN_CONFIG_FILE` | External YAML config override | *(none)* |

## Coding Style

- Java 21, 4-space indent, no tabs.
- Packages: `com.glodon.mordor.yansen.*` by concern (`config`, `llm`, `model`, `service`).
- Javadoc `@author` / `@date` / `@description` headers on class-level docs.

## Testing

No test framework is currently configured. JUnit 5 (`junit-jupiter`) must be added to `pom.xml` before writing tests. Test class naming: `<Class>Test.java`.

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
