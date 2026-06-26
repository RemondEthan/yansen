package com.glodon.mordor.yansen.llm.provider;

import com.glodon.mordor.yansen.config.ModelSettings;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleProviderTest {

    private static final ModelSettings VALID = new ModelSettings(
            "openai-compatible", "MiniMax-M3", "https://api.example.com/v1", "sk-test",
            null, null, null, null, null);

    private final OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider();

    @Test
    void nameIsStable() {
        assertEquals("openai-compatible", provider.name());
    }

    @Test
    void createReturnsOpenAiChatModelWithGivenModelName() {
        Model model = provider.create(VALID);
        assertNotNull(model);
        assertTrue(model instanceof OpenAIChatModel);
        assertEquals("MiniMax-M3", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void createAcceptsDifferentModelNames() {
        ModelSettings cfg = new ModelSettings(
                "openai-compatible", "deepseek-chat", "https://api.deepseek.com/v1", "sk-ds",
                null, null, null, null, null);
        Model model = provider.create(cfg);
        assertEquals("deepseek-chat", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void createRejectsBlankModelName() {
        ModelSettings cfg = new ModelSettings("openai-compatible", "  ", "url", "key",
                null, null, null, null, null);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> provider.create(cfg));
        assertTrue(e.getMessage().contains("modelName"));
    }

    @Test
    void createRejectsBlankApiKey() {
        ModelSettings cfg = new ModelSettings("openai-compatible", "m", "url", "",
                null, null, null, null, null);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> provider.create(cfg));
        assertTrue(e.getMessage().contains("apiKey"));
    }

    @Test
    void createRejectsBlankBaseUrl() {
        ModelSettings cfg = new ModelSettings("openai-compatible", "m", null, "key",
                null, null, null, null, null);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> provider.create(cfg));
        assertTrue(e.getMessage().contains("baseUrl"));
    }

    @Test
    void createRejectsNullSettings() {
        assertThrows(NullPointerException.class, () -> provider.create(null));
    }
}
