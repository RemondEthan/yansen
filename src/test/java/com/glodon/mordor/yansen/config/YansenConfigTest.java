package com.glodon.mordor.yansen.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class YansenConfigTest {

    private static YansenSettings parse(String yaml) {
        return YansenConfig.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesServerAndDatabaseSections() {
        String yaml = """
                server:
                  port: 9090
                  maxRequestSizeBytes: 50000
                database:
                  type: sqlite
                  sqlite:
                    path: /data/yansen.db
                """;
        YansenSettings settings = parse(yaml);

        assertEquals(9090, settings.serverOrDefault().portOrDefault());
        assertEquals(50_000L, settings.serverOrDefault().maxRequestSizeBytesOrDefault());
        assertEquals("sqlite", settings.databaseOrDefault().typeOrDefault());
        assertEquals("/data/yansen.db", settings.databaseOrDefault().sqliteOrDefault().pathOrDefault());
    }

    @Test
    void appliesServerDefaultsWhenSectionMissing() {
        YansenSettings settings = parse("database:\n  type: sqlite\n");
        assertEquals(ServerSettings.DEFAULT_PORT, settings.serverOrDefault().portOrDefault());
    }

    @Test
    void appliesDatabaseDefaultsWhenSectionMissing() {
        YansenSettings settings = parse("server:\n  port: 8080\n");
        assertEquals(SqliteSettings.DEFAULT_PATH, settings.databaseOrDefault().sqliteOrDefault().pathOrDefault());
        assertEquals(DatabaseSettings.TYPE_SQLITE, settings.databaseOrDefault().typeOrDefault());
    }

    @Test
    void resolvesPlaceholdersWithDefaultValue() {
        String yaml = """
                server:
                  port: "${YANSEN_TEST_PORT:7777}"
                database:
                  sqlite:
                    path: "${YANSEN_TEST_SQLITE_PATH:/tmp/test.db}"
                """;
        YansenSettings settings = parse(yaml);
        assertEquals(7777, settings.serverOrDefault().portOrDefault());
        assertEquals("/tmp/test.db", settings.databaseOrDefault().sqliteOrDefault().pathOrDefault());
    }

    @Test
    void mergedWithOverridesServerAndDatabase() {
        YansenSettings base = new YansenSettings(
                new ServerSettings(8080, 10_000L, 10L, null, null, null),
                new DatabaseSettings("sqlite", new SqliteSettings("a.db"), null));
        YansenSettings override = new YansenSettings(
                new ServerSettings(9090, 50_000L, 10L, null, null, null),
                new DatabaseSettings("sqlite", new SqliteSettings("b.db"), null));

        YansenSettings merged = base.mergedWith(override);

        assertEquals(9090, merged.serverOrDefault().portOrDefault());
        assertEquals("b.db", merged.databaseOrDefault().sqliteOrDefault().pathOrDefault());
    }

    @Test
    void mergedWithNullReturnsSelf() {
        YansenSettings base = new YansenSettings(
                new ServerSettings(8080, 10_000L, 10L, null, null, null), null);
        assertEquals(base, base.mergedWith(null));
    }

    @Test
    void ignoresUnknownYamlKeys() {
        YansenSettings settings = parse("""
                server:
                  port: 8080
                models:
                  legacy: ignored
                """);
        assertEquals(8080, settings.serverOrDefault().portOrDefault());
        assertNull(settings.database());
    }
}
