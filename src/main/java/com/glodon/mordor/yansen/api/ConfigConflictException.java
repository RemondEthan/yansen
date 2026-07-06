package com.glodon.mordor.yansen.api;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Thrown when a config create/update violates a uniqueness constraint.
 * Mapped to HTTP 409 by {@link GlobalExceptionMapper}.
 */
public class ConfigConflictException extends RuntimeException {

    public ConfigConflictException(String message) {
        super(message);
    }
}
