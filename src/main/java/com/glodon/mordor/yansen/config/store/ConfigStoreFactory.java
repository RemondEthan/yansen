package com.glodon.mordor.yansen.config.store;

import com.glodon.mordor.yansen.config.DatabaseSettings;
import com.glodon.mordor.yansen.config.store.sqlite.SqliteConfigStore;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Creates {@link ConfigStore} instances from {@link DatabaseSettings}.
 * Only {@link DatabaseSettings#TYPE_SQLITE} is supported at runtime; MySQL is reserved for future use.
 */
public final class ConfigStoreFactory {

    private ConfigStoreFactory() {
    }

    public static ConfigStore open(DatabaseSettings database) {
        DatabaseSettings db = database == null ? new DatabaseSettings(null, null, null).databaseOrDefault()
                : database.databaseOrDefault();
        return switch (db.typeOrDefault()) {
            case DatabaseSettings.TYPE_SQLITE ->
                    new SqliteConfigStore(db.sqliteOrDefault().pathOrDefault());
            case DatabaseSettings.TYPE_MYSQL ->
                    throw new UnsupportedOperationException(
                            "MySQL ConfigStore is planned but not implemented yet. "
                                    + "Use database.type=sqlite (default). "
                                    + "MySQL connection keys in yansen.yml are reserved for a future release; "
                                    + "see db/mysql/schema.sql.");
            default -> throw new IllegalArgumentException("Unknown database type: " + db.type());
        };
    }
}
