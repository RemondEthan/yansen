package com.glodon.mordor.yansen.skill;

import com.glodon.mordor.yansen.config.store.SkillConfigRecord;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Maps skill source ids (as written under agent.skills in YAML) to
 * {@link AgentSkillRepository} instances. Two schemes:
 * <ul>
 *   <li>{@code classpath:<base>} — pre-registered at bootstrap from
 *       {@link SkillsConfig#classpathBase()}; shared across all agents since classpath
 *       resources are read-only and identical for everyone.</li>
 *   <li>{@code workspace:<rel-path>} — built on demand against the calling agent's workspace
 *       at resolve time; the registry never holds a reference to the resulting repository.</li>
 * </ul>
 * <p>Workspace is intentionally a resolve-time concern, not a bootstrap-time one: each agent
 * has its own workspace, so a registry instance cannot be bound to one. The
 * {@code SkillsConfig.workspaceDir} field is therefore unused here — it was the old leaky
 * abstraction and is ignored.</p>
 */
public record SkillRegistry(Map<String, AgentSkillRepository> classpathRepos) {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    /**
     * Prefix for a classpath-resident skill source (looked up in {@link #classpathRepos}).
     */
    public static final String CLASSPATH_PREFIX = "classpath:";
    /**
     * Prefix for a workspace-resident skill source (built on demand from the agent's workspace).
     */
    public static final String WORKSPACE_PREFIX = "workspace:";

    public SkillRegistry(Map<String, AgentSkillRepository> classpathRepos) {
        Objects.requireNonNull(classpathRepos, "classpathRepos");
        this.classpathRepos = Map.copyOf(classpathRepos);
    }

    public boolean has(String id) {
        return classpathRepos.containsKey(id);
    }

    public Set<String> registeredSkillSources() {
        return Collections.unmodifiableSet(new TreeSet<>(classpathRepos.keySet()));
    }

    /**
     * Resolve a list of skill source ids to {@link AgentSkillRepository} instances, preserving
     * order. {@code classpath:<base>} entries are looked up in this registry; {@code
     * workspace:<rel-path>} entries are built on the fly from {@code workspacePath/<rel-path>}.
     *
     * @param ids           skill source ids; may be null or empty
     * @param workspacePath the calling agent's workspace path; required for any
     *                      {@code workspace:} id and must be a non-blank string
     * @return ordered list of repositories; empty when {@code ids} is null/empty
     * @throws IllegalStateException if a {@code workspace:} id is given but
     *                               {@code workspacePath} is null/blank, if a classpath id is unknown, if the id scheme is
     *                               not recognised, or if the id itself is null/blank
     */
    public List<AgentSkillRepository> resolve(Collection<String> ids, String workspacePath) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<AgentSkillRepository> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            out.add(resolveOne(id, workspacePath));
        }
        return Collections.unmodifiableList(out);
    }

    private AgentSkillRepository resolveOne(String id, String workspacePath) {
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("skill source id must not be null or blank");
        }
        if (id.startsWith(CLASSPATH_PREFIX)) {
            AgentSkillRepository repo = classpathRepos.get(id);
            if (repo == null) {
                throw new IllegalStateException(
                        "No classpath skill source registered for '" + id
                                + "'. Registered: " + registeredSkillSources());
            }
            return repo;
        }
        if (id.startsWith(WORKSPACE_PREFIX)) {
            if (workspacePath == null || workspacePath.isBlank()) {
                throw new IllegalStateException(
                        "skill source '" + id
                                + "' requires the calling agent's workspace, but none was provided");
            }
            String rel = id.substring(WORKSPACE_PREFIX.length());
            Path base = Paths.get(workspacePath, rel);
            log.debug("building FileSystemSkillRepository for skill source '{}' at {}", id, base);
            return new FileSystemSkillRepository(base);
        }
        throw new IllegalStateException(
                "Unknown skill source scheme in '" + id
                        + "'. Expected one of: " + CLASSPATH_PREFIX + ", " + WORKSPACE_PREFIX);
    }

    /**
     * Maps a skill_config row to the resolve id understood by {@link #resolve(Collection, String)}.
     */
    public static String toResolveId(SkillConfigRecord record) {
        return switch (record.sourceType()) {
            case "classpath" -> CLASSPATH_PREFIX + record.sourceRef();
            case "workspace" -> WORKSPACE_PREFIX + record.sourceRef();
            default -> throw new IllegalStateException(
                    "Unknown skill sourceType '" + record.sourceType() + "' for skill '" + record.skillId() + "'");
        };
    }

    /**
     * Pre-register classpath skill repositories declared in the config database.
     */
    public static SkillRegistry fromSkillRecords(List<SkillConfigRecord> records) {
        if (records == null || records.isEmpty()) {
            return new SkillRegistry(Map.of());
        }
        Map<String, AgentSkillRepository> map = new LinkedHashMap<>();
        for (SkillConfigRecord record : records) {
            if (!"classpath".equals(record.sourceType())) {
                continue;
            }
            String resolveId = toResolveId(record);
            if (map.containsKey(resolveId)) {
                continue;
            }
            try {
                map.put(resolveId, new ClasspathSkillRepository(record.sourceRef()));
                log.info("registered classpath skill source '{}' from skill '{}'", resolveId, record.skillId());
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to build classpath skill repository for skill '" + record.skillId()
                                + "' base '" + record.sourceRef() + "'", e);
            }
        }
        return new SkillRegistry(map);
    }

    /**
     * Pre-register a single classpath base (used in tests).
     */
    public static SkillRegistry fromClasspathBase(String classpathBase) {
        if (classpathBase == null || classpathBase.isBlank()) {
            return new SkillRegistry(Map.of());
        }
        return fromSkillRecords(List.of(new SkillConfigRecord(
                "legacy", "Legacy", "classpath", classpathBase, null, null)));
    }
}
