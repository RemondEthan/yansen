package com.glodon.mordor.yansen.api;

/**
 * @author: Remond
 * @date: 2026-06-29
 * @description: Thrown when an upstream LLM service fails to respond within the configured
 * timeout. Mapped to HTTP 504 Gateway Timeout by {@link GlobalExceptionMapper}.
 */
public final class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message) {
        super(message);
    }

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
