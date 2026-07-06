package com.glodon.mordor.yansen.api;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Shared helpers for config CRUD error translation.
 */
final class ConfigApiSupport {

    private ConfigApiSupport() {
    }

    static <T> T runStore(StoreOperation<T> operation) {
        try {
            return operation.run();
        } catch (RuntimeException e) {
            throw translate(e);
        }
    }

    static void runStoreVoid(StoreVoidOperation operation) {
        try {
            operation.run();
        } catch (RuntimeException e) {
            throw translate(e);
        }
    }

    private static RuntimeException translate(RuntimeException e) {
        Throwable cause = unwrap(e);
        if (cause instanceof SQLIntegrityConstraintViolationException constraint) {
            String message = constraint.getMessage();
            if (message != null && message.contains("FOREIGN KEY constraint failed")) {
                return new IllegalArgumentException("Referenced entity does not exist");
            }
            return new ConfigConflictException(message == null ? "Unique constraint violated" : message);
        }
        if (cause instanceof SQLException sql) {
            String message = sql.getMessage();
            if (message != null && message.contains("FOREIGN KEY constraint failed")) {
                return new IllegalArgumentException("Referenced entity does not exist");
            }
        }
        return e;
    }

    private static Throwable unwrap(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    @FunctionalInterface
    interface StoreOperation<T> {
        T run();
    }

    @FunctionalInterface
    interface StoreVoidOperation {
        void run();
    }
}
