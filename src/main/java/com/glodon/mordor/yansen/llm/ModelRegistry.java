package com.glodon.mordor.yansen.llm;

import com.glodon.mordor.yansen.config.ModelSettings;
import io.agentscope.core.model.Model;

import java.util.Iterator;
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
 * @description: Maps provider names (as written under model.provider in YAML) to
 * {@link ModelProvider} instances. Discovery is plug-in driven: implementations are found
 * on the classpath via {@link ServiceLoader} from
 * {@code META-INF/services/com.glodon.mordor.yansen.llm.ModelProvider}. Built-in providers
 * (e.g. {@code OpenAiCompatibleProvider}) ship with the application; third-party providers
 * are added by dropping a jar on the classpath. Owns no business state; thread-safe and
 * immutable after construction.
 */
public final class ModelRegistry {

    private final Map<String, ModelProvider> providers;

    public ModelRegistry(Map<String, ModelProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("ModelRegistry requires at least one provider");
        }
        this.providers = Map.copyOf(providers);
    }

    public ModelProvider provider(String name) {
        ModelProvider p = providers.get(name);
        if (p == null) {
            throw new IllegalStateException(
                    "No model provider registered for '" + name + "'. Registered: " + registeredProviders());
        }
        return p;
    }

    public Model create(ModelSettings settings) {
        Objects.requireNonNull(settings, "settings");
        if (settings.provider() == null || settings.provider().isBlank()) {
            throw new IllegalStateException("ModelSettings.provider is required");
        }
        return provider(settings.provider()).create(settings);
    }

    public Set<String> registeredProviders() {
        return Collections.unmodifiableSet(new TreeSet<>(providers.keySet()));
    }

    /**
     * Discover {@link ModelProvider} implementations via the current thread's context
     * classloader (which is what {@link ServiceLoader#load(Class)} uses). Fails fast on
     * empty discovery and on duplicate names — both indicate a packaging / classpath bug
     * that should surface at startup, not at first request.
     */
    public static ModelRegistry discover() {
        return discover(ServiceLoader.load(ModelProvider.class));
    }

    /**
     * Build a registry from an explicit {@link Iterable} of providers. Package-private so
     * tests can inject fixtures without polluting the SPI file; the production path goes
     * through {@link #discover()}.
     */
    static ModelRegistry discover(Iterable<ModelProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        Map<String, ModelProvider> map = new LinkedHashMap<>();
        try {
            for (Iterator<ModelProvider> it = providers.iterator(); it.hasNext(); ) {
                ModelProvider p = it.next();
                Objects.requireNonNull(p, "ModelProvider from SPI");
                String name = p.name();
                if (name == null || name.isBlank()) {
                    throw new IllegalStateException(
                            "ModelProvider " + p.getClass().getName() + " returned a blank name()");
                }
                ModelProvider existing = map.put(name, p);
                if (existing != null) {
                    throw new IllegalStateException(
                            "Duplicate ModelProvider name '" + name + "': "
                                    + existing.getClass().getName() + " vs " + p.getClass().getName());
                }
            }
        } catch (ServiceConfigurationError e) {
            throw new IllegalStateException(
                    "Failed to load ModelProvider implementations from the classpath", e);
        }
        if (map.isEmpty()) {
            throw new IllegalStateException(
                    "No ModelProvider implementations found on the classpath. "
                            + "Add a provider to META-INF/services/"
                            + "com.glodon.mordor.yansen.llm.ModelProvider or check the packaging.");
        }
        return new ModelRegistry(map);
    }
}
