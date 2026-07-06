package com.glodon.mordor.yansen.config.store.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Callback for work executed inside a single JDBC transaction.
 */
@FunctionalInterface
interface SqlConnectionConsumer {

    void accept(Connection conn) throws SQLException;
}
