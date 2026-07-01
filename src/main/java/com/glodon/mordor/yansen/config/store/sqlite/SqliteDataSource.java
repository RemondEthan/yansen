package com.glodon.mordor.yansen.config.store.sqlite;

import com.glodon.mordor.yansen.config.store.AppDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: HikariCP connection pool for SQLite.
 * SQLite supports only a single writer, so maximumPoolSize is fixed at 1.
 * WAL mode is enabled on startup to improve concurrent read performance.
 */
public class SqliteDataSource implements AppDataSource {
    
    private final HikariDataSource delegate;
    
    public SqliteDataSource(String dbPath) {
        Path dbFile = Path.of(dbPath);
        Path parentDir = dbFile.getParent();
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to create database directory: " + parentDir, e);
            }
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile);
        config.setPoolName("SqlitePool");
        
        // SQLite write concurrency limit: only one writer allowed
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        
        // Connection acquisition timeout
        config.setConnectionTimeout(30000);
        
        // Idle timeout: 0 disables idle retirement (HikariCP rejects negative values)
        config.setIdleTimeout(0);
        
        // Max connection lifetime: Long.MAX_VALUE disables it
        config.setMaxLifetime(Long.MAX_VALUE);
        
        // Allow startup even if pool can't connect immediately
        config.setInitializationFailTimeout(-1);
        
        config.setConnectionInitSql(
            "PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON"
        );
        
        this.delegate = new HikariDataSource(config);
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }
    
    @Override
    public void close() {
        delegate.close();
    }
}
