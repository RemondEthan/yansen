package com.glodon.mordor.yansen.tool;

import io.agentscope.core.tool.Toolkit;

import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Maps tool ids (as written under agent.tools in YAML) to {@link ToolProvider}
 * instances. Discovery is plug-in driven: implementations are found on the classpath via
 * {@link ServiceLoader} from {@code META-INF/services/com.glodon.mordor.yansen.tool.ToolProvider}.
 * Built-in tools (e.g. {@code DateTimeTool}) ship with the application; third-party tools
 * are added by dropping a jar on the classpath.
 *
 * <p>A provider usually <em>is</em> the tool bean (its {@code @Tool}-annotated methods
 * become the individually invocable tools); {@link ToolProvider#toolBean()} defaults to
 * returning the provider itself for that case.</p>
 */
public final class ToolRegistry {

    private final Map<String, ToolProvider> tools;

    public ToolRegistry(Map<String, ToolProvider> tools) {
        Objects.requireNonNull(tools, "tools");
        if (tools.isEmpty()) {
            throw new IllegalArgumentException("ToolRegistry requires at least one tool provider");
        }
        this.tools = Map.copyOf(tools);
    }

    public boolean has(String id) {
        return tools.containsKey(id);
    }

    public ToolProvider get(String id) {
        ToolProvider provider = tools.get(id);
        if (provider == null) {
            throw new IllegalStateException(
                    "No tool registered with id '" + id + "'. Registered: " + registeredTools());
        }
        return provider;
    }

    public Set<String> registeredTools() {
        return Collections.unmodifiableSet(new TreeSet<>(tools.keySet()));
    }

    /**
     * Build a {@link Toolkit} containing exactly the tools referenced by {@code ids}.
     * An empty (or null) id collection yields an empty Toolkit. Each registered id is
     * resolved to its provider's {@link ToolProvider#toolBean() bean} before registration.
     */
    public Toolkit buildToolkit(Collection<String> ids) {
        Toolkit toolkit = new Toolkit();
        if (ids == null || ids.isEmpty()) {
            return toolkit;
        }
        for (String id : ids) {
            toolkit.registerTool(get(id).toolBean());
        }
        return toolkit;
    }

    /**
     * Discover {@link ToolProvider} implementations via the current thread's context
     * classloader. Fails fast on empty discovery and on duplicate ids.
     */
    public static ToolRegistry discover() {
        return discover(ServiceLoader.load(ToolProvider.class));
    }

    /**
     * Build a registry from an explicit {@link Iterable} of providers. Package-private so
     * tests can inject fixtures without polluting the SPI file; the production path goes
     * through {@link #discover()}.
     */
    static ToolRegistry discover(Iterable<ToolProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        Map<String, ToolProvider> map = new LinkedHashMap<>();
        try {
            for (Iterator<ToolProvider> it = providers.iterator(); it.hasNext(); ) {
                ToolProvider p = it.next();
                Objects.requireNonNull(p, "ToolProvider from SPI");
                String id = p.toolId();
                if (id == null || id.isBlank()) {
                    throw new IllegalStateException(
                            "ToolProvider " + p.getClass().getName() + " returned a blank toolId()");
                }
                ToolProvider existing = map.put(id, p);
                if (existing != null) {
                    throw new IllegalStateException(
                            "Duplicate ToolProvider id '" + id + "': "
                                    + existing.getClass().getName() + " vs " + p.getClass().getName());
                }
            }
        } catch (ServiceConfigurationError e) {
            throw new IllegalStateException(
                    "Failed to load ToolProvider implementations from the classpath", e);
        }
        if (map.isEmpty()) {
            throw new IllegalStateException(
                    "No ToolProvider implementations found on the classpath. "
                            + "Add a provider to META-INF/services/"
                            + "com.glodon.mordor.yansen.tool.ToolProvider or check the packaging.");
        }
        return new ToolRegistry(map);
    }
}
