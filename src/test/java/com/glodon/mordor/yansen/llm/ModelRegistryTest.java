package com.glodon.mordor.yansen.llm;

import com.glodon.mordor.yansen.config.ModelSettings;
import com.glodon.mordor.yansen.llm.ModelProvider;
import com.glodon.mordor.yansen.llm.provider.OpenAiCompatibleProvider;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelRegistryTest {

    @Test
    void discoverFindsBuiltInOpenAiCompatible() {
        ModelRegistry registry = ModelRegistry.discover();
        assertEquals(Set.of("openai-compatible"), registry.registeredProviders());
    }

    @Test
    void discoverFindsExactlyOneBuiltInProvider() {
        // SPI ships with exactly the OpenAI-compatible adapter; any future built-in must
        // be a deliberate addition, not a side effect of a jar.
        ModelRegistry registry = ModelRegistry.discover();
        assertEquals(1, registry.registeredProviders().size());
    }

    @Test
    void discoverFromIterableRejectsEmpty() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ModelRegistry.discover(List.of()));
        assertTrue(e.getMessage().contains("No ModelProvider"));
    }

    @Test
    void discoverFromIterableRejectsNullName() {
        ModelProvider blank = new ModelProvider() {
            @Override public String name() { return "  "; }
            @Override public Model create(ModelSettings settings) { throw new UnsupportedOperationException(); }
        };
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ModelRegistry.discover(List.of(blank)));
        assertTrue(e.getMessage().contains("blank"));
    }

    @Test
    void discoverFromIterableRejectsDuplicateName() {
        OpenAiCompatibleProvider a = new OpenAiCompatibleProvider();
        OpenAiCompatibleProvider b = new OpenAiCompatibleProvider();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ModelRegistry.discover(List.of(a, b)));
        assertTrue(e.getMessage().contains("Duplicate"));
        assertTrue(e.getMessage().contains("openai-compatible"));
    }

    @Test
    void discoverFromIterableAcceptsMixedProviders() {
        OpenAiCompatibleProvider a = new OpenAiCompatibleProvider();
        ModelProvider custom = new ModelProvider() {
            @Override public String name() { return "custom"; }
            @Override public Model create(ModelSettings settings) { throw new UnsupportedOperationException(); }
        };
        ModelRegistry registry = ModelRegistry.discover(List.of(a, custom));
        assertEquals(Set.of("openai-compatible", "custom"), registry.registeredProviders());
    }

    @Test
    void providerReturnsRegistered() {
        OpenAiCompatibleProvider custom = new OpenAiCompatibleProvider();
        ModelRegistry registry = new ModelRegistry(Map.of("custom", custom));
        assertSame(custom, registry.provider("custom"));
    }

    @Test
    void providerThrowsForUnknownName() {
        ModelRegistry registry = ModelRegistry.discover();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registry.provider("anthropic"));
        assertTrue(e.getMessage().contains("anthropic"));
        assertTrue(e.getMessage().contains("Registered"));
    }

    @Test
    void createDispatchesBySettingsProvider() {
        ModelRegistry registry = ModelRegistry.discover();
        ModelSettings cfg = new ModelSettings(
                "openai-compatible", "MiniMax-M3", "https://api.minimaxi.com/v1", "sk-test",
                null, null, null, null, null);
        Model model = registry.create(cfg);
        assertNotNull(model);
        assertEquals("MiniMax-M3", ((io.agentscope.core.model.OpenAIChatModel) model).getModelName());
    }

    @Test
    void createRejectsNullSettings() {
        ModelRegistry registry = ModelRegistry.discover();
        assertThrows(NullPointerException.class, () -> registry.create(null));
    }

    @Test
    void createRejectsBlankProvider() {
        ModelRegistry registry = ModelRegistry.discover();
        ModelSettings cfg = new ModelSettings("  ", "m", "url", "key",
                null, null, null, null, null);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> registry.create(cfg));
        assertTrue(e.getMessage().contains("provider"));
    }

    @Test
    void rejectsEmptyProviderMap() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ModelRegistry(Map.of()));
        assertTrue(e.getMessage().contains("at least one"));
    }

    @Test
    void rejectsNullProviderMap() {
        assertThrows(NullPointerException.class, () -> new ModelRegistry(null));
    }

    @Test
    void registeredProvidersIsImmutable() {
        ModelRegistry registry = ModelRegistry.discover();
        assertThrows(UnsupportedOperationException.class,
                () -> registry.registeredProviders().add("rogue"));
    }
}
