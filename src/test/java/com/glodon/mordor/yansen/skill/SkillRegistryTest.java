package com.glodon.mordor.yansen.skill;

import com.glodon.mordor.yansen.config.store.SkillConfigRecord;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRegistryTest {

    private static final String CLASSPATH_KEY = "classpath:skills";

    @Test
    void fromClasspathConfigWithClasspathBaseRegistersRepo() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        assertTrue(registry.has(CLASSPATH_KEY));
        assertEquals(Set.of(CLASSPATH_KEY), registry.registeredSkillSources());
    }

    @Test
    void fromClasspathConfigWithNullConfigReturnsEmpty() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        assertEquals(0, registry.registeredSkillSources().size());
    }

    @Test
    void fromClasspathConfigWithoutClasspathBaseReturnsEmpty() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        assertEquals(0, registry.registeredSkillSources().size());
    }

    @Test
    void fromClasspathConfigWithBlankClasspathBaseReturnsEmpty() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("   ");
        assertEquals(0, registry.registeredSkillSources().size());
    }

    @Test
    void fromClasspathConfigIgnoresWorkspaceDir() {
        // The registry must NOT bake workspaceDir into itself; the workspace is per-agent.
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        assertEquals(Set.of(CLASSPATH_KEY), registry.registeredSkillSources());
    }

    @Test
    void hasReturnsFalseForUnknown() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        assertFalse(registry.has("classpath:nope"));
        assertFalse(registry.has("workspace:foo"));
    }

    @Test
    void registeredSkillSourcesIsImmutable() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        Set<String> sources = registry.registeredSkillSources();
        assertThrows(UnsupportedOperationException.class, () -> sources.add("rogue"));
    }

    @Test
    void resolveEmptyReturnsEmpty() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        List<AgentSkillRepository> out = registry.resolve(List.of(), "/tmp/anywhere");
        assertEquals(0, out.size());
    }

    @Test
    void resolveNullReturnsEmpty() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        List<AgentSkillRepository> out = registry.resolve(null, "/tmp/anywhere");
        assertEquals(0, out.size());
    }

    @Test
    void resolveClasspathIdReturnsRegisteredInstance() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        List<AgentSkillRepository> out = registry.resolve(List.of(CLASSPATH_KEY), "/tmp/anywhere");
        assertEquals(1, out.size());
        assertNotNull(out.get(0));
        assertTrue(out.get(0) instanceof ClasspathSkillRepository);
    }

    @Test
    void resolveClasspathIdIsIdentityForPreRegisteredRepo() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        AgentSkillRepository preRegistered = registry.resolve(List.of(CLASSPATH_KEY), "/tmp").get(0);
        AgentSkillRepository again = registry.resolve(List.of(CLASSPATH_KEY), "/tmp").get(0);
        assertSame(preRegistered, again);
    }

    @Test
    void resolveClasspathIdThrowsForUnknownId() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registry.resolve(List.of("classpath:nope"), "/tmp"));
        assertTrue(e.getMessage().contains("classpath:nope"));
    }

    @Test
    void resolveWorkspaceIdBuildsRepoOnDemand(@TempDir Path tempDir) throws Exception {
        Path subDir = Files.createDirectory(tempDir.resolve("skills"));
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        List<AgentSkillRepository> out = registry.resolve(List.of("workspace:skills"), tempDir.toString());
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof FileSystemSkillRepository);
    }

    @Test
    void resolveWorkspaceIdDoesNotRegisterInTheRegistry(@TempDir Path tempDir) throws Exception {
        // resolve() of a workspace id must not leak entries into registeredSkillSources.
        Files.createDirectory(tempDir.resolve("skills"));
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        registry.resolve(List.of("workspace:skills"), tempDir.toString());
        assertEquals(0, registry.registeredSkillSources().size());
    }

    @Test
    void resolveWorkspaceIdThrowsWhenWorkspacePathIsNull() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registry.resolve(List.of("workspace:skills"), null));
        assertTrue(e.getMessage().contains("workspace:skills"));
    }

    @Test
    void resolveWorkspaceIdThrowsWhenWorkspacePathIsBlank() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        assertThrows(IllegalStateException.class,
                () -> registry.resolve(List.of("workspace:skills"), "  "));
    }

    @Test
    void resolveUnknownSchemeThrows() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registry.resolve(List.of("git:foo"), "/tmp"));
        assertTrue(e.getMessage().contains("git:foo"));
    }

    @Test
    void resolveBlankIdThrows() {
        SkillRegistry registry = SkillRegistry.fromClasspathBase(null);
        assertThrows(IllegalStateException.class, () -> registry.resolve(List.of(""), "/tmp"));
    }

    @Test
    void resolveMixedIdsPreservesOrder(@TempDir Path tempDir) throws Exception {
        Files.createDirectory(tempDir.resolve("skills"));
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        List<AgentSkillRepository> out = registry.resolve(
                List.of("classpath:skills", "workspace:skills", "classpath:skills"), tempDir.toString());
        assertEquals(3, out.size());
        assertTrue(out.get(0) instanceof ClasspathSkillRepository);
        assertTrue(out.get(1) instanceof FileSystemSkillRepository);
        assertTrue(out.get(2) instanceof ClasspathSkillRepository);
    }

    @Test
    void resolveResultIsUnmodifiable() throws Exception {
        SkillRegistry registry = SkillRegistry.fromClasspathBase("skills");
        List<AgentSkillRepository> out = registry.resolve(List.of(CLASSPATH_KEY), "/tmp");
        AgentSkillRepository rogue = new ClasspathSkillRepository("skills");
        assertThrows(UnsupportedOperationException.class, () -> out.add(rogue));
    }

    @Test
    void rejectsNullClasspathMap() {
        assertThrows(NullPointerException.class, () -> new SkillRegistry(null));
    }

    @Test
    void customRegistryResolvesById() throws Exception {
        AgentSkillRepository custom = new ClasspathSkillRepository("skills");
        SkillRegistry registry = new SkillRegistry(Map.of("classpath:custom", custom));
        List<AgentSkillRepository> out = registry.resolve(List.of("classpath:custom"), "/tmp");
        assertEquals(1, out.size());
        assertSame(custom, out.get(0));
    }

    @Test
    void toResolveId_resolvesSourceRefPlaceholder() {
        SkillConfigRecord record = new SkillConfigRecord(
                "s1", "Skills", "classpath", "${SKILLS_BASE:skills}", null, null);
        assertEquals("classpath:skills", SkillRegistry.toResolveId(record));
    }
}
