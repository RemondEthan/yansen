package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: HTTP server settings (port, max request size, etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerSettings(Integer port, Long maxRequestSizeBytes, Long keepAliveIntervalSeconds,
                              Long sseIdleTimeoutSeconds) {

    public static final int DEFAULT_PORT = 8080;
    public static final long DEFAULT_MAX_REQUEST_SIZE_BYTES = 10_000L;
    /**
     * Default heartbeat interval (seconds) for SSE streams. Sits well below Jetty's
     * HTTP idle timeout so a long thinking/CoT pause cannot silently cut the connection.
     * Set to 0 in YAML/config to disable heartbeats.
     */
    public static final long DEFAULT_KEEP_ALIVE_INTERVAL_SECONDS = 10L;
    /**
     * Default SSE idle timeout (seconds) — the Jetty connector idle timeout applied to
     * SSE connections. Must be large enough that long model thinking/CoT pauses don't
     * cut the stream. The heartbeat keeps the wire active within this window.
     */
    public static final long DEFAULT_SSE_IDLE_TIMEOUT_SECONDS = 300L;

    public ServerSettings {
        if (port == null) {
            port = DEFAULT_PORT;
        }
        if (maxRequestSizeBytes == null) {
            maxRequestSizeBytes = DEFAULT_MAX_REQUEST_SIZE_BYTES;
        }
        if (keepAliveIntervalSeconds == null) {
            keepAliveIntervalSeconds = DEFAULT_KEEP_ALIVE_INTERVAL_SECONDS;
        } else if (keepAliveIntervalSeconds < 0) {
            keepAliveIntervalSeconds = 0L;
        }
        if (sseIdleTimeoutSeconds == null) {
            sseIdleTimeoutSeconds = DEFAULT_SSE_IDLE_TIMEOUT_SECONDS;
        } else if (sseIdleTimeoutSeconds < 0) {
            sseIdleTimeoutSeconds = DEFAULT_SSE_IDLE_TIMEOUT_SECONDS;
        }
    }

    public int portOrDefault() {
        return port == null ? DEFAULT_PORT : port;
    }

    public long maxRequestSizeBytesOrDefault() {
        return maxRequestSizeBytes == null ? DEFAULT_MAX_REQUEST_SIZE_BYTES : maxRequestSizeBytes;
    }

    public long keepAliveIntervalSecondsOrDefault() {
        return keepAliveIntervalSeconds == null
                ? DEFAULT_KEEP_ALIVE_INTERVAL_SECONDS
                : keepAliveIntervalSeconds;
    }

    public boolean keepAliveEnabled() {
        return keepAliveIntervalSecondsOrDefault() > 0L;
    }

    public long sseIdleTimeoutSecondsOrDefault() {
        return sseIdleTimeoutSeconds == null
                ? DEFAULT_SSE_IDLE_TIMEOUT_SECONDS
                : sseIdleTimeoutSeconds;
    }
}
