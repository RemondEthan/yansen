package com.glodon.mordor.yansen.config;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Raised when the configuration cannot be loaded or parsed.
 */
public class YansenConfigException extends RuntimeException {

    public YansenConfigException(String message) {
        super(message);
    }

    public YansenConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
