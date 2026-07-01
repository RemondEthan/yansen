package com.glodon.mordor.yansen.registry;

import com.glodon.mordor.yansen.agent.AgentFactory;
import com.glodon.mordor.yansen.agent.AgentNotFoundException;
import com.glodon.mordor.yansen.agent.YansenAgent;
import com.glodon.mordor.yansen.config.store.AgentConfigRecord;
import com.glodon.mordor.yansen.config.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Lazy-loads and caches {@link YansenAgent} instances. Loaded agents stay in memory.
 */
public final class AgentRegistry implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

    private final ConfigStore configStore;
    private final AgentFactory agentFactory;
    private final ConcurrentHashMap<String, YansenAgent> cache = new ConcurrentHashMap<>();

    public AgentRegistry(ConfigStore configStore, AgentFactory agentFactory) {
        this.configStore = configStore;
        this.agentFactory = agentFactory;
    }

    public YansenAgent get(String agentId) {
        YansenAgent cached = cache.get(agentId);
        if (cached != null) {
            return cached;
        }
        AgentConfigRecord record = configStore.getAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException("agent '" + agentId + "' is not configured"));
        YansenAgent created = agentFactory.create(record);
        YansenAgent existing = cache.putIfAbsent(agentId, created);
        if (existing != null) {
            created.close();
            return existing;
        }
        return created;
    }

    public void invalidate(String agentId) {
        YansenAgent removed = cache.remove(agentId);
        if (removed != null) {
            log.info("Invalidated cached agent '{}'", agentId);
            removed.close();
        }
    }

    public List<String> listCachedAgentIds() {
        return List.copyOf(cache.keySet());
    }

    public int cachedCount() {
        return cache.size();
    }

    @Override
    public void close() {
        for (Map.Entry<String, YansenAgent> entry : cache.entrySet()) {
            try {
                entry.getValue().close();
            } catch (RuntimeException e) {
                log.warn("Failed to close agent '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        cache.clear();
    }
}
