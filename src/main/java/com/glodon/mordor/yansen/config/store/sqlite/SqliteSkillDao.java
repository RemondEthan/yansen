package com.glodon.mordor.yansen.config.store.sqlite;
import com.glodon.mordor.yansen.config.store.AppDataSource;

import com.glodon.mordor.yansen.config.store.SkillConfigRecord;
import com.glodon.mordor.yansen.config.store.dao.SkillDao;
import com.glodon.mordor.yansen.config.store.dao.SkillDao.Field;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: SQLite implementation of SkillDao.
 */
public class SqliteSkillDao extends SqliteDao<SkillConfigRecord> implements SkillDao {
    
    public SqliteSkillDao(AppDataSource dataSource) {
        super(dataSource);
    }
    
    @Override
    public List<SkillConfigRecord> list() {
        try (Connection conn = dataSource.getConnection()) {
            return query(conn, SkillDao.SELECT_ALL);
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + SkillDao.SELECT_ALL, e);
        }
    }
    
    @Override
    public Optional<SkillConfigRecord> get(String skillId) {
        try (Connection conn = dataSource.getConnection()) {
            List<SkillConfigRecord> results = query(conn, SkillDao.SELECT_BY_ID, skillId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + SkillDao.SELECT_BY_ID, e);
        }
    }
    
    @Override
    public SkillConfigRecord create(SkillConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, SkillDao.INSERT,
                record.skillId(), record.name(), record.sourceType(), record.sourceRef(), now, now);
            conn.commit();
            return new SkillConfigRecord(record.skillId(), record.name(), record.sourceType(), record.sourceRef(), now, now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + SkillDao.INSERT, e);
        }
    }
    
    @Override
    public SkillConfigRecord update(SkillConfigRecord record) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            String now = now();
            execute(conn, SkillDao.UPDATE,
                record.name(), record.sourceType(), record.sourceRef(), now, record.skillId());
            conn.commit();
            return new SkillConfigRecord(record.skillId(), record.name(), record.sourceType(), record.sourceRef(),
                record.createdAt(), now);
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + SkillDao.UPDATE, e);
        }
    }
    
    @Override
    public void delete(String skillId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            execute(conn, SkillDao.DELETE, skillId);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + SkillDao.DELETE, e);
        }
    }
    
    @Override
    protected SkillConfigRecord parse(ResultSet rs) throws SQLException {
        return new SkillConfigRecord(
            rs.getString(Field.SKILL_ID),
            rs.getString(Field.NAME),
            rs.getString(Field.SOURCE_TYPE),
            rs.getString(Field.SOURCE_REF),
            rs.getString(Field.CREATED_AT),
            rs.getString(Field.UPDATED_AT));
    }
}
