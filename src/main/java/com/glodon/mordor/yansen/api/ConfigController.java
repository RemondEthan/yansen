package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.api.dto.config.AgentConfigRequest;
import com.glodon.mordor.yansen.api.dto.config.McpConfigRequest;
import com.glodon.mordor.yansen.api.dto.config.ModelConfigRequest;
import com.glodon.mordor.yansen.api.dto.config.PromptConfigRequest;
import com.glodon.mordor.yansen.api.dto.config.SkillConfigRequest;
import com.glodon.mordor.yansen.api.dto.config.ToolConfigRequest;
import com.glodon.mordor.yansen.config.store.AgentConfigRecord;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import com.glodon.mordor.yansen.config.store.McpConfigRecord;
import com.glodon.mordor.yansen.config.store.ModelConfigRecord;
import com.glodon.mordor.yansen.config.store.SkillConfigRecord;
import com.glodon.mordor.yansen.config.store.SystemPromptRecord;
import com.glodon.mordor.yansen.config.store.ToolConfigRecord;
import com.glodon.mordor.yansen.registry.AgentRegistry;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: REST CRUD for business configuration stored in {@link ConfigStore}.
 * Values are persisted literally (including {@code ${ENV:default}} placeholders);
 * env substitution happens only when agents are instantiated.
 */
public final class ConfigController implements Controller {

    private final ConfigStore configStore;
    private final AgentRegistry agentRegistry;
    private final AgentRouteRegistrar routeRegistrar;
    private final RoutesConfig routes;

    public ConfigController(ConfigStore configStore,
                            AgentRegistry agentRegistry,
                            AgentRouteRegistrar routeRegistrar,
                            RoutesConfig routes) {
        this.configStore = configStore;
        this.agentRegistry = agentRegistry;
        this.routeRegistrar = routeRegistrar;
        this.routes = routes;
    }

    @Override
    public void registerOn(RoutesConfig routesConfig) {
        routesConfig.apiBuilder((EndpointGroup) () -> ApiBuilder.path("api/config", this::registerConfigRoutes));
    }

    private void registerConfigRoutes() {
        ApiBuilder.path("model", () -> {
            ApiBuilder.get(this::listModels);
            ApiBuilder.post(this::createModel);
            ApiBuilder.get("{id}", this::getModel);
            ApiBuilder.put("{id}", this::updateModel);
            ApiBuilder.delete("{id}", this::deleteModel);
        });
        ApiBuilder.path("agent", () -> {
            ApiBuilder.get(this::listAgents);
            ApiBuilder.post(this::createAgent);
            ApiBuilder.get("{id}", this::getAgent);
            ApiBuilder.put("{id}", this::updateAgent);
            ApiBuilder.delete("{id}", this::deleteAgent);
        });
        ApiBuilder.path("prompt", () -> {
            ApiBuilder.get(this::listPrompts);
            ApiBuilder.post(this::createPrompt);
            ApiBuilder.get("{id}", this::getPrompt);
            ApiBuilder.put("{id}", this::updatePrompt);
            ApiBuilder.delete("{id}", this::deletePrompt);
        });
        ApiBuilder.path("tool", () -> {
            ApiBuilder.get(this::listTools);
            ApiBuilder.post(this::createTool);
            ApiBuilder.get("{id}", this::getTool);
            ApiBuilder.put("{id}", this::updateTool);
            ApiBuilder.delete("{id}", this::deleteTool);
        });
        ApiBuilder.path("skill", () -> {
            ApiBuilder.get(this::listSkills);
            ApiBuilder.post(this::createSkill);
            ApiBuilder.get("{id}", this::getSkill);
            ApiBuilder.put("{id}", this::updateSkill);
            ApiBuilder.delete("{id}", this::deleteSkill);
        });
        ApiBuilder.path("mcp", () -> {
            ApiBuilder.get(this::listMcp);
            ApiBuilder.post(this::createMcp);
            ApiBuilder.get("{id}", this::getMcp);
            ApiBuilder.put("{id}", this::updateMcp);
            ApiBuilder.delete("{id}", this::deleteMcp);
        });
    }

    // ---- Model ----

    private void listModels(Context ctx) {
        ctx.json(configStore.listModels());
    }

    private void getModel(Context ctx) {
        String id = ctx.pathParam("id");
        ctx.json(requireModel(id));
    }

    private void createModel(Context ctx) {
        ModelConfigRequest req = ctx.bodyAsClass(ModelConfigRequest.class);
        validateModelRequest(req);
        if (req.modelId() == null || req.modelId().isBlank()) {
            throw new IllegalArgumentException("modelId must not be blank");
        }
        if (configStore.getModel(req.modelId()).isPresent()) {
            throw new ConfigConflictException("model '" + req.modelId() + "' already exists");
        }
        ModelConfigRecord created = ConfigApiSupport.runStore(() -> configStore.createModel(toModelRecord(req)));
        ctx.status(201).json(created);
    }

    private void updateModel(Context ctx) {
        String id = ctx.pathParam("id");
        ModelConfigRecord existing = requireModel(id);
        ModelConfigRequest req = ctx.bodyAsClass(ModelConfigRequest.class);
        validateModelRequest(req);
        ModelConfigRecord updated = ConfigApiSupport.runStore(() -> configStore.updateModel(
                new ModelConfigRecord(
                        id,
                        req.provider(),
                        req.modelName(),
                        req.baseUrl(),
                        req.apiKey(),
                        req.maxRetries(),
                        req.connectTimeoutSeconds(),
                        req.readTimeoutSeconds(),
                        req.writeTimeoutSeconds(),
                        existing.createdAt(),
                        null)));
        ctx.json(updated);
    }

    private void deleteModel(Context ctx) {
        String id = ctx.pathParam("id");
        requireModel(id);
        ConfigApiSupport.runStoreVoid(() -> configStore.deleteModel(id));
        ctx.status(204);
    }

    // ---- Agent ----

    private void listAgents(Context ctx) {
        ctx.json(configStore.listAgents());
    }

    private void getAgent(Context ctx) {
        ctx.json(requireAgent(ctx.pathParam("id")));
    }

    private void createAgent(Context ctx) {
        AgentConfigRequest req = ctx.bodyAsClass(AgentConfigRequest.class);
        validateAgentRequest(req);
        if (req.agentId() == null || req.agentId().isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (configStore.getAgent(req.agentId()).isPresent()) {
            throw new ConfigConflictException("agent '" + req.agentId() + "' already exists");
        }
        validateAgentReferences(req);
        AgentConfigRecord record = toAgentRecord(req);
        AgentConfigRecord created = ConfigApiSupport.runStore(() -> configStore.createAgent(record));
        routeRegistrar.registerRoute(routes, created.route(), created.agentId());
        ctx.status(201).json(created);
    }

    private void updateAgent(Context ctx) {
        String id = ctx.pathParam("id");
        AgentConfigRecord existing = requireAgent(id);
        AgentConfigRequest req = ctx.bodyAsClass(AgentConfigRequest.class);
        validateAgentRequest(req);
        validateAgentReferences(req);

        String oldRoute = existing.route();
        String newRoute = req.route();
        if (!oldRoute.equals(newRoute)) {
            routeRegistrar.unregisterRoute(oldRoute);
            routeRegistrar.registerRoute(routes, newRoute, id);
        }

        AgentConfigRecord updated = ConfigApiSupport.runStore(() -> configStore.updateAgent(
                new AgentConfigRecord(
                        id,
                        req.name(),
                        req.agentType(),
                        newRoute,
                        req.modelId(),
                        req.systemPromptId(),
                        req.workspace(),
                        req.tools(),
                        req.skills(),
                        req.mcp(),
                        existing.createdAt(),
                        null)));
        agentRegistry.invalidate(id);
        ctx.json(updated);
    }

    private void deleteAgent(Context ctx) {
        String id = ctx.pathParam("id");
        AgentConfigRecord existing = requireAgent(id);
        ConfigApiSupport.runStoreVoid(() -> configStore.deleteAgent(id));
        routeRegistrar.unregisterRoute(existing.route());
        agentRegistry.invalidate(id);
        ctx.status(204);
    }

    // ---- Prompt ----

    private void listPrompts(Context ctx) {
        ctx.json(configStore.listPrompts());
    }

    private void getPrompt(Context ctx) {
        ctx.json(requirePrompt(parsePromptId(ctx)));
    }

    private void createPrompt(Context ctx) {
        PromptConfigRequest req = ctx.bodyAsClass(PromptConfigRequest.class);
        validatePromptRequest(req);
        SystemPromptRecord created = ConfigApiSupport.runStore(() -> configStore.createPrompt(
                new SystemPromptRecord(null, req.name(), req.sourceType(), req.sourceRef(), null, null)));
        ctx.status(201).json(created);
    }

    private void updatePrompt(Context ctx) {
        int id = parsePromptId(ctx);
        SystemPromptRecord existing = requirePrompt(id);
        PromptConfigRequest req = ctx.bodyAsClass(PromptConfigRequest.class);
        validatePromptRequest(req);
        SystemPromptRecord updated = ConfigApiSupport.runStore(() -> configStore.updatePrompt(
                new SystemPromptRecord(id, req.name(), req.sourceType(), req.sourceRef(),
                        existing.createdAt(), null)));
        ctx.json(updated);
    }

    private void deletePrompt(Context ctx) {
        int id = parsePromptId(ctx);
        requirePrompt(id);
        ConfigApiSupport.runStoreVoid(() -> configStore.deletePrompt(id));
        ctx.status(204);
    }

    // ---- Tool ----

    private void listTools(Context ctx) {
        ctx.json(configStore.listTools());
    }

    private void getTool(Context ctx) {
        ctx.json(requireTool(ctx.pathParam("id")));
    }

    private void createTool(Context ctx) {
        ToolConfigRequest req = ctx.bodyAsClass(ToolConfigRequest.class);
        validateToolRequest(req);
        if (req.toolId() == null || req.toolId().isBlank()) {
            throw new IllegalArgumentException("toolId must not be blank");
        }
        if (configStore.getTool(req.toolId()).isPresent()) {
            throw new ConfigConflictException("tool '" + req.toolId() + "' already exists");
        }
        ToolConfigRecord created = ConfigApiSupport.runStore(() -> configStore.createTool(toToolRecord(req)));
        ctx.status(201).json(created);
    }

    private void updateTool(Context ctx) {
        String id = ctx.pathParam("id");
        ToolConfigRecord existing = requireTool(id);
        ToolConfigRequest req = ctx.bodyAsClass(ToolConfigRequest.class);
        validateToolRequest(req);
        ToolConfigRecord updated = ConfigApiSupport.runStore(() -> configStore.updateTool(
                new ToolConfigRecord(
                        id,
                        req.name(),
                        req.description(),
                        req.enabled() != null && req.enabled(),
                        existing.createdAt(),
                        null)));
        ctx.json(updated);
    }

    private void deleteTool(Context ctx) {
        String id = ctx.pathParam("id");
        requireTool(id);
        ConfigApiSupport.runStoreVoid(() -> configStore.deleteTool(id));
        ctx.status(204);
    }

    // ---- Skill ----

    private void listSkills(Context ctx) {
        ctx.json(configStore.listSkills());
    }

    private void getSkill(Context ctx) {
        ctx.json(requireSkill(ctx.pathParam("id")));
    }

    private void createSkill(Context ctx) {
        SkillConfigRequest req = ctx.bodyAsClass(SkillConfigRequest.class);
        validateSkillRequest(req);
        if (req.skillId() == null || req.skillId().isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        if (configStore.getSkill(req.skillId()).isPresent()) {
            throw new ConfigConflictException("skill '" + req.skillId() + "' already exists");
        }
        SkillConfigRecord created = ConfigApiSupport.runStore(() -> configStore.createSkill(toSkillRecord(req)));
        ctx.status(201).json(created);
    }

    private void updateSkill(Context ctx) {
        String id = ctx.pathParam("id");
        SkillConfigRecord existing = requireSkill(id);
        SkillConfigRequest req = ctx.bodyAsClass(SkillConfigRequest.class);
        validateSkillRequest(req);
        SkillConfigRecord updated = ConfigApiSupport.runStore(() -> configStore.updateSkill(
                new SkillConfigRecord(
                        id,
                        req.name(),
                        req.sourceType(),
                        req.sourceRef(),
                        existing.createdAt(),
                        null)));
        ctx.json(updated);
    }

    private void deleteSkill(Context ctx) {
        String id = ctx.pathParam("id");
        requireSkill(id);
        ConfigApiSupport.runStoreVoid(() -> configStore.deleteSkill(id));
        ctx.status(204);
    }

    // ---- MCP ----

    private void listMcp(Context ctx) {
        ctx.json(configStore.listMcp());
    }

    private void getMcp(Context ctx) {
        ctx.json(requireMcp(ctx.pathParam("id")));
    }

    private void createMcp(Context ctx) {
        McpConfigRequest req = ctx.bodyAsClass(McpConfigRequest.class);
        validateMcpRequest(req);
        if (req.mcpId() == null || req.mcpId().isBlank()) {
            throw new IllegalArgumentException("mcpId must not be blank");
        }
        if (configStore.getMcp(req.mcpId()).isPresent()) {
            throw new ConfigConflictException("mcp '" + req.mcpId() + "' already exists");
        }
        McpConfigRecord created = ConfigApiSupport.runStore(() -> configStore.createMcp(toMcpRecord(req)));
        ctx.status(201).json(created);
    }

    private void updateMcp(Context ctx) {
        String id = ctx.pathParam("id");
        McpConfigRecord existing = requireMcp(id);
        McpConfigRequest req = ctx.bodyAsClass(McpConfigRequest.class);
        validateMcpRequest(req);
        McpConfigRecord updated = ConfigApiSupport.runStore(() -> configStore.updateMcp(
                new McpConfigRecord(id, req.name(), req.config(), existing.createdAt(), null)));
        ctx.json(updated);
    }

    private void deleteMcp(Context ctx) {
        String id = ctx.pathParam("id");
        requireMcp(id);
        ConfigApiSupport.runStoreVoid(() -> configStore.deleteMcp(id));
        ctx.status(204);
    }

    // ---- Helpers ----

    private ModelConfigRecord requireModel(String id) {
        return configStore.getModel(id)
                .orElseThrow(() -> new ConfigNotFoundException("model '" + id + "' not found"));
    }

    private AgentConfigRecord requireAgent(String id) {
        return configStore.getAgent(id)
                .orElseThrow(() -> new ConfigNotFoundException("agent '" + id + "' not found"));
    }

    private SystemPromptRecord requirePrompt(int id) {
        return configStore.getPrompt(id)
                .orElseThrow(() -> new ConfigNotFoundException("prompt '" + id + "' not found"));
    }

    private ToolConfigRecord requireTool(String id) {
        return configStore.getTool(id)
                .orElseThrow(() -> new ConfigNotFoundException("tool '" + id + "' not found"));
    }

    private SkillConfigRecord requireSkill(String id) {
        return configStore.getSkill(id)
                .orElseThrow(() -> new ConfigNotFoundException("skill '" + id + "' not found"));
    }

    private McpConfigRecord requireMcp(String id) {
        return configStore.getMcp(id)
                .orElseThrow(() -> new ConfigNotFoundException("mcp '" + id + "' not found"));
    }

    private static int parsePromptId(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("prompt id must be an integer");
        }
    }

    private void validateAgentReferences(AgentConfigRequest req) {
        if (configStore.getModel(req.modelId()).isEmpty()) {
            throw new IllegalArgumentException("model '" + req.modelId() + "' not found");
        }
        if (req.systemPromptId() != null && configStore.getPrompt(req.systemPromptId()).isEmpty()) {
            throw new IllegalArgumentException("prompt '" + req.systemPromptId() + "' not found");
        }
        if (req.tools() != null) {
            for (String toolId : req.tools()) {
                if (configStore.getTool(toolId).isEmpty()) {
                    throw new IllegalArgumentException("tool '" + toolId + "' not found");
                }
            }
        }
        if (req.skills() != null) {
            for (String skillId : req.skills()) {
                if (configStore.getSkill(skillId).isEmpty()) {
                    throw new IllegalArgumentException("skill '" + skillId + "' not found");
                }
            }
        }
        if (req.mcp() != null) {
            for (String mcpId : req.mcp()) {
                if (configStore.getMcp(mcpId).isEmpty()) {
                    throw new IllegalArgumentException("mcp '" + mcpId + "' not found");
                }
            }
        }
    }

    private static void validateModelRequest(ModelConfigRequest req) {
        requireNonBlank(req.provider(), "provider");
        requireNonBlank(req.modelName(), "modelName");
        requireNonBlank(req.baseUrl(), "baseUrl");
        if (req.apiKey() == null) {
            throw new IllegalArgumentException("apiKey must not be null");
        }
    }

    private static void validateAgentRequest(AgentConfigRequest req) {
        requireNonBlank(req.name(), "name");
        requireNonBlank(req.agentType(), "agentType");
        requireNonBlank(req.route(), "route");
        requireNonBlank(req.modelId(), "modelId");
        requireNonBlank(req.workspace(), "workspace");
    }

    private static void validatePromptRequest(PromptConfigRequest req) {
        requireNonBlank(req.name(), "name");
        requireNonBlank(req.sourceType(), "sourceType");
        if (req.sourceRef() == null) {
            throw new IllegalArgumentException("sourceRef must not be null");
        }
    }

    private static void validateToolRequest(ToolConfigRequest req) {
        requireNonBlank(req.name(), "name");
        if (req.description() == null) {
            throw new IllegalArgumentException("description must not be null");
        }
    }

    private static void validateSkillRequest(SkillConfigRequest req) {
        requireNonBlank(req.name(), "name");
        requireNonBlank(req.sourceType(), "sourceType");
        if (req.sourceRef() == null) {
            throw new IllegalArgumentException("sourceRef must not be null");
        }
    }

    private static void validateMcpRequest(McpConfigRequest req) {
        requireNonBlank(req.name(), "name");
        if (req.config() == null) {
            throw new IllegalArgumentException("config must not be null");
        }
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static ModelConfigRecord toModelRecord(ModelConfigRequest req) {
        return new ModelConfigRecord(
                req.modelId(),
                req.provider(),
                req.modelName(),
                req.baseUrl(),
                req.apiKey(),
                req.maxRetries(),
                req.connectTimeoutSeconds(),
                req.readTimeoutSeconds(),
                req.writeTimeoutSeconds(),
                null,
                null);
    }

    private static AgentConfigRecord toAgentRecord(AgentConfigRequest req) {
        return new AgentConfigRecord(
                req.agentId(),
                req.name(),
                req.agentType(),
                req.route(),
                req.modelId(),
                req.systemPromptId(),
                req.workspace(),
                req.tools(),
                req.skills(),
                req.mcp(),
                null,
                null);
    }

    private static ToolConfigRecord toToolRecord(ToolConfigRequest req) {
        return new ToolConfigRecord(
                req.toolId(),
                req.name(),
                req.description(),
                req.enabled() == null || req.enabled(),
                null,
                null);
    }

    private static SkillConfigRecord toSkillRecord(SkillConfigRequest req) {
        return new SkillConfigRecord(
                req.skillId(),
                req.name(),
                req.sourceType(),
                req.sourceRef(),
                null,
                null);
    }

    private static McpConfigRecord toMcpRecord(McpConfigRequest req) {
        return new McpConfigRecord(req.mcpId(), req.name(), req.config(), null, null);
    }
}
