package com.glodon.mordor.yansen.config.store.sqlite;

import com.glodon.mordor.yansen.config.store.AppDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Abstract base for all SQLite DAO implementations.
 * Manages connection acquisition from the pool and transaction lifecycle.
 * Subclasses provide only parse() for ResultSet → domain object mapping.
 *
 * <p>Two transaction patterns:</p>
 * <ul>
 *   <li><b>Standalone public methods</b> — acquire a connection and delegate to
 *       {@link SqliteTransactions#inTransaction} for a single-table write.</li>
 *   <li><b>{@code void op(Connection conn, ...)} package methods</b> — execute SQL only;
 *       the caller ({@link SqliteConfigStore} or {@link SqliteTransactions}) owns
 *       commit/rollback. These methods must not call {@code setAutoCommit}, {@code commit},
 *       or {@code rollback}.</li>
 * </ul>
 *
 * @param <T> the domain record type (e.g., ModelConfigRecord)
 */
public abstract class SqliteDao<T> {
    
    protected final AppDataSource dataSource;
    
    protected SqliteDao(AppDataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected static String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
    
    /**
     * Executes a SELECT query and collects all matching records.
     */
    protected List<T> query(Connection conn, String sql, Object... params) {
        try (PreparedStatement stmt = prepare(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            ArrayList<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(parse(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }
    
    /**
     * Executes an INSERT/UPDATE/DELETE statement.
     */
    protected void execute(Connection conn, String sql, Object... params) {
        try (PreparedStatement stmt = prepare(conn, sql, params)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + sql, e);
        }
    }
    
    /**
     * Binds params to a PreparedStatement (no generated-keys).
     */
    protected PreparedStatement prepare(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt;
    }
    
    /**
     * Executes an INSERT and returns the auto-generated key.
     */
    protected int executeAndReturnKey(Connection conn, String sql, Object... params) {
        try (PreparedStatement stmt = prepareWithGeneratedKeys(conn, sql, params)) {
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new RuntimeException("No generated key returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + sql, e);
        }
    }
    
    /**
     * Binds params to a PreparedStatement that returns generated keys.
     */
    protected PreparedStatement prepareWithGeneratedKeys(Connection conn, String sql, Object... params)
            throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt;
    }
    
    /**
     * Maps a ResultSet row to a domain record. Implemented by each concrete DAO.
     */
    protected abstract T parse(ResultSet rs) throws SQLException;
}
