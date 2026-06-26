package com.glodon.mordor.yansen.tool;

import com.glodon.mordor.yansen.tool.builtin.DateTimeTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    @Test
    void discoverFindsBuiltInDateTime() {
        ToolRegistry registry = ToolRegistry.discover();
        assertTrue(registry.has("datetime"));
        assertEquals(Set.of("datetime"), registry.registeredTools());
    }

    @Test
    void discoverFindsExactlyOneBuiltInTool() {
        ToolRegistry registry = ToolRegistry.discover();
        assertEquals(1, registry.registeredTools().size());
    }

    @Test
    void getReturnsRegisteredProvider() {
        ToolRegistry registry = ToolRegistry.discover();
        ToolProvider provider = registry.get("datetime");
        assertNotNull(provider);
        assertTrue(provider instanceof DateTimeTool);
        assertSame(provider, provider.toolBean());
    }

    @Test
    void getThrowsForUnknownId() {
        ToolRegistry registry = ToolRegistry.discover();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registry.get("nope"));
        assertTrue(e.getMessage().contains("nope"));
        assertTrue(e.getMessage().contains("datetime"));
    }

    @Test
    void discoverFromIterableRejectsEmpty() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ToolRegistry.discover(List.of()));
        assertTrue(e.getMessage().contains("No ToolProvider"));
    }

    @Test
    void discoverFromIterableRejectsBlankId() {
        ToolProvider blank = new ToolProvider() {
            @Override public String toolId() { return ""; }
        };
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ToolRegistry.discover(List.of(blank)));
        assertTrue(e.getMessage().contains("blank"));
    }

    @Test
    void discoverFromIterableRejectsDuplicateId() {
        ToolProvider a = new DateTimeTool();
        ToolProvider b = new DateTimeTool();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ToolRegistry.discover(List.of(a, b)));
        assertTrue(e.getMessage().contains("Duplicate"));
        assertTrue(e.getMessage().contains("datetime"));
    }

    @Test
    void discoverFromIterableAcceptsCustomProviderWithSeparateBean() {
        // A provider that contributes a different bean than itself.
        Object customBean = new Object() {
            @Tool(name = "noop", description = "does nothing")
            public String noop() { return "ok"; }
        };
        ToolProvider wrapping = new ToolProvider() {
            @Override public String toolId() { return "wrapping"; }
            @Override public Object toolBean() { return customBean; }
        };
        ToolRegistry registry = ToolRegistry.discover(List.of(wrapping));
        assertEquals(Set.of("wrapping"), registry.registeredTools());
        assertSame(customBean, registry.get("wrapping").toolBean());
    }

    @Test
    void rejectsNullMap() {
        assertThrows(NullPointerException.class, () -> new ToolRegistry(null));
    }

    @Test
    void rejectsEmptyMap() {
        assertThrows(IllegalArgumentException.class, () -> new ToolRegistry(Map.of()));
    }

    @Test
    void buildToolkitRegistersAllAnnotatedMethods() {
        ToolRegistry registry = ToolRegistry.discover();
        Toolkit toolkit = registry.buildToolkit(List.of("datetime"));

        assertTrue(toolkit.getToolNames().contains("current_datetime"));
        assertTrue(toolkit.getToolNames().contains("current_date"));
    }

    @Test
    void buildToolkitWithEmptyIdsReturnsEmptyToolkit() {
        ToolRegistry registry = ToolRegistry.discover();
        Toolkit toolkit = registry.buildToolkit(List.of());
        assertEquals(0, toolkit.getToolNames().size());
    }

    @Test
    void buildToolkitWithNullReturnsEmptyToolkit() {
        ToolRegistry registry = ToolRegistry.discover();
        Toolkit toolkit = registry.buildToolkit(null);
        assertEquals(0, toolkit.getToolNames().size());
    }

    @Test
    void buildToolkitThrowsForUnknownId() {
        ToolRegistry registry = ToolRegistry.discover();
        assertThrows(IllegalStateException.class, () -> registry.buildToolkit(List.of("nope")));
    }

    @Test
    void hasReturnsFalseForUnknownId() {
        ToolRegistry registry = ToolRegistry.discover();
        assertFalse(registry.has("nope"));
    }

    @Test
    void registeredToolsIsImmutable() {
        ToolRegistry registry = ToolRegistry.discover();
        assertThrows(UnsupportedOperationException.class,
                () -> registry.registeredTools().add("rogue"));
    }
}
