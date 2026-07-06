package com.glodon.mordor.yansen.api.dto.config;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Request body for model config create/update.
 */
public record ModelConfigRequest(
        String modelId,
        String provider,
        String modelName,
        String baseUrl,
        String apiKey,
        Integer maxRetries,
        Integer connectTimeoutSeconds,
        Integer readTimeoutSeconds,
        Integer writeTimeoutSeconds) {
}
