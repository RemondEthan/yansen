package com.glodon.mordor.yansen.config.store.sqlite;

import com.glodon.mordor.yansen.config.store.AppDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Shared transaction boundary for SQLite config persistence.
 * Callers pass a {@link SqlConnectionConsumer} that performs one or more statements on the
 * same connection; commit/rollback and {@code autoCommit} restoration are handled here.
 */
final class SqliteTransactions {

    private SqliteTransactions() {
    }

    static void inTransaction(AppDataSource dataSource, String operation, SqlConnectionConsumer work) {
        try (Connection conn = dataSource.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                work.accept(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to " + operation, e);
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to " + operation, e);
        }
    }
}
