package com.glodon.mordor.yansen.config.store.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Executes classpath SQL scripts against SQLite. Strips line comments,
 * splits on statement terminators ({@code ;}), and runs each statement individually because
 * the SQLite JDBC driver does not reliably execute multi-statement strings in one call.
 *
 * <p>Script conventions: use {@code --} line comments only; terminate each statement with
 * {@code ;}; do not embed {@code ;} inside string literals.</p>
 */
final class SqlScriptExecutor {

    private SqlScriptExecutor() {
    }

    static List<String> parseStatements(String script) {
        String withoutComments = stripLineComments(script);
        List<String> statements = new ArrayList<>();
        for (String part : withoutComments.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }

    static void execute(Connection conn, String script) throws SQLException {
        for (String sql : parseStatements(script)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    private static String stripLineComments(String script) {
        StringBuilder out = new StringBuilder();
        for (String line : script.split("\n", -1)) {
            int commentStart = indexOfLineComment(line);
            out.append(commentStart >= 0 ? line.substring(0, commentStart) : line);
            out.append('\n');
        }
        return out.toString();
    }

    /**
     * Returns the index of {@code --} that starts a line comment, ignoring {@code --}
     * that appears inside single-quoted string literals.
     */
    static int indexOfLineComment(String line) {
        boolean inSingleQuote = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            if (c == '\'') {
                if (inSingleQuote && i + 1 < line.length() && line.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (!inSingleQuote && line.charAt(i) == '-' && line.charAt(i + 1) == '-') {
                return i;
            }
        }
        return -1;
    }
}
