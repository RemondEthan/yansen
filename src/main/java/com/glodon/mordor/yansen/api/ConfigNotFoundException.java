package com.glodon.mordor.yansen.api;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Thrown when a requested config entity does not exist. Mapped to HTTP 404.
 */
public class ConfigNotFoundException extends RuntimeException {

    public ConfigNotFoundException(String message) {
        super(message);
    }
}
