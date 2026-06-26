package com.glodon.mordor.yansen.llm;

import com.glodon.mordor.yansen.config.ModelSettings;
import io.agentscope.core.model.Model;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Factory that turns a {@link ModelSettings} entry into a concrete agentscope
 * {@link Model}. Each provider owns its own protocol and credential handling.
 * Provider instances are stateless and may be registered under multiple names in
 * {@link ModelRegistry} (e.g. an OpenAI-compatible instance registered as both
 * "openai-compatible" and "minimax").
 */
public interface ModelProvider {

    /**
     * Canonical provider identifier (e.g. "openai-compatible").
     * Informational only; dispatch goes through {@link ModelRegistry#create(ModelSettings)}.
     */
    String name();

    /**
     * Build a {@link Model} from settings. Implementations should validate required
     * fields and throw {@link IllegalStateException} with a clear message on misconfiguration.
     */
    Model create(ModelSettings settings);
}
