package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Per-model provider configuration.
 * For PR1 only the openai-compatible family is wired in; additional providers slot in during PR2.
 * Timeout fields default to null — the provider then falls back to framework defaults.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelSettings(
        String provider,
        String modelName,
        String baseUrl,
        String apiKey,
        @Deprecated Integer timeoutSeconds,
        Integer maxRetries,
        Integer connectTimeoutSeconds,
        Integer readTimeoutSeconds,
        Integer writeTimeoutSeconds) {
}
