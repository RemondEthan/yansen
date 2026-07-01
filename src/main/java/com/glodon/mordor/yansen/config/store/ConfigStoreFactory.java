package com.glodon.mordor.yansen.config.store;

import com.glodon.mordor.yansen.config.DatabaseSettings;
import com.glodon.mordor.yansen.config.store.sqlite.SqliteConfigStore;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Creates {@link ConfigStore} instances from {@link DatabaseSettings}.
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
                            "MySQL config store is not implemented yet; set database.type=sqlite");
            default -> throw new IllegalArgumentException("Unknown database type: " + db.type());
        };
    }
}
