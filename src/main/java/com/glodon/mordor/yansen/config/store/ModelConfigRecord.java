package com.glodon.mordor.yansen.config.store;

import com.glodon.mordor.yansen.config.ModelSettings;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Persistence type for model_config table. Bridges to {@link ModelSettings}
 * via {@link #toModelSettings()}.
 */
public record ModelConfigRecord(
        String modelId,
        String provider,
        String modelName,
        String baseUrl,
        String apiKey,
        Integer maxRetries,
        Integer connectTimeoutSeconds,
        Integer readTimeoutSeconds,
        Integer writeTimeoutSeconds,
        String createdAt,
        String updatedAt) {

    /**
     * Convert to the SPI contract type for agent runtime.
     * Resolves {@code ${ENV:default}} placeholders in {@code baseUrl} and {@code apiKey}
     * using current environment variables. The database record itself is not modified.
     */
    public ModelSettings toModelSettings() {
        return new ModelSettings(
                provider,
                modelName,
                ConfigValueResolver.resolveStored(baseUrl),
                ConfigValueResolver.resolveStored(apiKey),
                null, // timeoutSeconds deprecated
                maxRetries, connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds);
    }
}
