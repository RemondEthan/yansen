package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: HTTP server settings (port, max request size, etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerSettings(Integer port, Long maxRequestSizeBytes, Long keepAliveIntervalSeconds) {

    public static final int DEFAULT_PORT = 8080;
    public static final long DEFAULT_MAX_REQUEST_SIZE_BYTES = 10_000L;
    /**
     * Default heartbeat interval (seconds) for SSE streams. Sits well below Jetty's 30s
     * HTTP idle timeout so a long thinking/CoT pause cannot silently cut the connection.
     * Set to 0 in YAML/config to disable heartbeats.
     */
    public static final long DEFAULT_KEEP_ALIVE_INTERVAL_SECONDS = 15L;

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
            // Negative values are nonsensical; treat as "disabled".
            keepAliveIntervalSeconds = 0L;
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
}
