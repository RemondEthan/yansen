package com.glodon.mordor.yansen.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSettingsTest {

    @Test
    void appliesDefaultsWhenAllFieldsNull() {
        ServerSettings s = new ServerSettings(null, null, null, null, null, null);
        assertEquals(ServerSettings.DEFAULT_PORT, s.portOrDefault());
        assertEquals(ServerSettings.DEFAULT_MAX_REQUEST_SIZE_BYTES, s.maxRequestSizeBytesOrDefault());
        assertEquals(ServerSettings.DEFAULT_KEEP_ALIVE_INTERVAL_SECONDS, s.keepAliveIntervalSecondsOrDefault());
        assertEquals(ServerSettings.DEFAULT_SSE_EVENT_TIMEOUT_SECONDS, s.sseEventTimeoutSecondsOrDefault());
        assertEquals(ServerSettings.DEFAULT_CHAT_TIMEOUT_SECONDS, s.chatTimeoutSecondsOrDefault());
        assertTrue(s.keepAliveEnabled());
    }

    @Test
    void keepAliveZeroDisablesHeartbeat() {
        ServerSettings s = new ServerSettings(8080, 10_000L, 0L, null, null, null);
        assertEquals(0L, s.keepAliveIntervalSecondsOrDefault());
        assertFalse(s.keepAliveEnabled());
    }

    @Test
    void negativeKeepAliveClampedToZero() {
        // Negative values are nonsensical; treat as "disabled" rather than letting a stray
        // negative config value crash the scheduler with IllegalArgumentException.
        ServerSettings s = new ServerSettings(8080, 10_000L, -5L, null, null, null);
        assertEquals(0L, s.keepAliveIntervalSecondsOrDefault());
        assertFalse(s.keepAliveEnabled());
    }

    @Test
    void positiveKeepAlivePreserved() {
        ServerSettings s = new ServerSettings(8080, 10_000L, 30L, null, null, null);
        assertEquals(30L, s.keepAliveIntervalSecondsOrDefault());
        assertTrue(s.keepAliveEnabled());
    }

    @Test
    void sseEventTimeoutAppliesDefault() {
        ServerSettings s = new ServerSettings(null, null, null, null, null, null);
        assertEquals(ServerSettings.DEFAULT_SSE_EVENT_TIMEOUT_SECONDS, s.sseEventTimeoutSecondsOrDefault());
    }

    @Test
    void sseEventTimeoutNegativeClampedToDefault() {
        ServerSettings s = new ServerSettings(null, null, null, null, -10L, null);
        assertEquals(ServerSettings.DEFAULT_SSE_EVENT_TIMEOUT_SECONDS, s.sseEventTimeoutSecondsOrDefault());
    }

    @Test
    void chatTimeoutAppliesDefault() {
        ServerSettings s = new ServerSettings(null, null, null, null, null, null);
        assertEquals(ServerSettings.DEFAULT_CHAT_TIMEOUT_SECONDS, s.chatTimeoutSecondsOrDefault());
    }

    @Test
    void chatTimeoutNegativeClampedToDefault() {
        ServerSettings s = new ServerSettings(null, null, null, null, null, -10L);
        assertEquals(ServerSettings.DEFAULT_CHAT_TIMEOUT_SECONDS, s.chatTimeoutSecondsOrDefault());
    }
}
