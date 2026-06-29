package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: HTTP server settings (port, max request size, etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerSettings(Integer port, Long maxRequestSizeBytes, Long keepAliveIntervalSeconds,
                              Long sseIdleTimeoutSeconds, Long sseEventTimeoutSeconds,
                              Long chatTimeoutSeconds) {
    private static final Logger log = LoggerFactory.getLogger(ServerSettings.class);

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
    /**
     * Default inter-event timeout (seconds) for SSE streams. If no event is received within
     * this window, the stream is considered stalled and terminated with an error. Prevents
     * zombie streams when the LLM API stops sending events but doesn't close the connection
     * (the heartbeat keeps the wire alive, so the idle timeout alone cannot detect this).
     */
    public static final long DEFAULT_SSE_EVENT_TIMEOUT_SECONDS = 120L;
    /**
     * Default total timeout (seconds) for synchronous chat requests. Limits how long
     * {@code .block()} waits before throwing, preventing a hung LLM from permanently
     * occupying a Jetty request thread.
     */
    public static final long DEFAULT_CHAT_TIMEOUT_SECONDS = 300L;

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
        if (sseEventTimeoutSeconds == null) {
            sseEventTimeoutSeconds = DEFAULT_SSE_EVENT_TIMEOUT_SECONDS;
        } else if (sseEventTimeoutSeconds < 0) {
            sseEventTimeoutSeconds = DEFAULT_SSE_EVENT_TIMEOUT_SECONDS;
        }
        if (chatTimeoutSeconds == null) {
            chatTimeoutSeconds = DEFAULT_CHAT_TIMEOUT_SECONDS;
        } else if (chatTimeoutSeconds < 0) {
            chatTimeoutSeconds = DEFAULT_CHAT_TIMEOUT_SECONDS;
        }
        // Heartbeat interval must be strictly less than the idle timeout, otherwise
        // Jetty's idle timeout fires before the first heartbeat tick reaches the wire,
        // silently cutting the connection during long thinking/CoT pauses. When the
        // interval equals or exceeds the timeout the heartbeat is completely ineffective
        // — it can never reset the idle timer. We correct to idleTimeout / 3 (rather
        // than / 2) to leave a comfortable margin for GC pauses, thread-scheduling
        // jitter, and TCP write latency. A value of 0 (heartbeat disabled) is left
        // untouched — the operator explicitly opted out.
        if (keepAliveIntervalSeconds > 0 && keepAliveIntervalSeconds >= sseIdleTimeoutSeconds) {
            long corrected = sseIdleTimeoutSeconds / 3;
            log.warn("keepAliveIntervalSeconds ({}s) >= sseIdleTimeoutSeconds ({}s); "
                     + "heartbeat would never fire before idle timeout cuts the connection. "
                     + "Corrected to {}s (idleTimeout / 3).",
                     keepAliveIntervalSeconds, sseIdleTimeoutSeconds, corrected);
            keepAliveIntervalSeconds = corrected;
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

    public long sseEventTimeoutSecondsOrDefault() {
        return sseEventTimeoutSeconds == null
                ? DEFAULT_SSE_EVENT_TIMEOUT_SECONDS
                : sseEventTimeoutSeconds;
    }

    public long chatTimeoutSecondsOrDefault() {
        return chatTimeoutSeconds == null
                ? DEFAULT_CHAT_TIMEOUT_SECONDS
                : chatTimeoutSeconds;
    }
}
