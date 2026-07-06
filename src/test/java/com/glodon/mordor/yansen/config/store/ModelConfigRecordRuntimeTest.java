package com.glodon.mordor.yansen.config.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelConfigRecordRuntimeTest {

    @Test
    void toModelSettings_resolvesApiKeyPlaceholder() {
        ModelConfigRecord stored = new ModelConfigRecord(
                "default", "openai-compatible", "MiniMax-M3", "https://api.example.com/v1",
                "${TEST_API_KEY:fallback-key}", 3, 30, 900, 30, null, null);

        // PlaceholderResolver uses System.getenv; verify via direct comparison with expected pattern
        var settings = stored.toModelSettings();
        // Without env set, default after colon is used
        assertEquals("fallback-key", settings.apiKey());
    }

    @Test
    void toModelSettings_resolvesModelNameAndProviderPlaceholders() {
        ModelConfigRecord stored = new ModelConfigRecord(
                "default", "${PROVIDER:openai-compatible}", "${MODEL_NAME:MiniMax-M3}",
                "https://api.example.com/v1", "sk-literal", null, null, null, null, null, null);

        var settings = stored.toModelSettings();
        assertEquals("openai-compatible", settings.provider());
        assertEquals("MiniMax-M3", settings.modelName());
    }

    @Test
    void toModelSettings_keepsLiteralApiKey() {
        ModelConfigRecord stored = new ModelConfigRecord(
                "m1", "openai-compatible", "gpt-4", "https://api.example.com/v1",
                "sk-literal", null, null, null, null, null, null);

        assertEquals("sk-literal", stored.toModelSettings().apiKey());
    }
}
