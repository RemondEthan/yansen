package com.glodon.mordor.yansen.config.store;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Unified DataSource interface for all database implementations.
 * Implementations manage connection pooling and lifecycle. DAO layer depends only on this interface.
 *
 * <p>Current implementations:</p>
 * <ul>
 *   <li>{@code SqliteDataSource} — SQLite with HikariCP, WAL mode enabled</li>
 *   <li>{@code MySqlDataSource} — MySQL with HikariCP (future)</li>
 * </ul>
 *
 * <p>Switching databases only requires changing the factory that creates the ConfigStore;
 * all DAO classes remain untouched.</p>
 */
public interface AppDataSource extends AutoCloseable {

    /**
     * Borrows a connection from the pool. Caller must close the connection
     * when done (try-with-resources recommended).
     */
    Connection getConnection() throws SQLException;

    @Override
    void close();
}
