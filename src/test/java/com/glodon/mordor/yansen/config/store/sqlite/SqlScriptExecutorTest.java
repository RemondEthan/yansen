package com.glodon.mordor.yansen.config.store.sqlite;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScriptExecutorTest {

    @Test
    void parseStatements_splitsOnSemicolonAndStripsComments() {
        String script = """
                -- header comment
                CREATE TABLE IF NOT EXISTS t1 (id INTEGER);
                INSERT INTO t1 VALUES (1); -- trailing comment
                """;

        List<String> statements = SqlScriptExecutor.parseStatements(script);

        assertEquals(2, statements.size());
        assertEquals("CREATE TABLE IF NOT EXISTS t1 (id INTEGER)", statements.get(0));
        assertEquals("INSERT INTO t1 VALUES (1)", statements.get(1));
    }

    @Test
    void indexOfLineComment_ignoresDoubleDashInsideStringLiteral() {
        String line = "VALUES ('a--b', 'still-data'); -- real comment";
        assertEquals(line.indexOf("-- real"), SqlScriptExecutor.indexOfLineComment(line));
    }

    @Test
    void parseStatements_preservesEscapedSingleQuoteInLiteral() {
        String script = "INSERT INTO t VALUES ('it''s ok');";

        List<String> statements = SqlScriptExecutor.parseStatements(script);

        assertEquals(1, statements.size());
        assertTrue(statements.getFirst().contains("it''s ok"));
    }
}
