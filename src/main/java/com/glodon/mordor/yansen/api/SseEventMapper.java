package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.api.dto.SseEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;

import java.util.Optional;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Translates an {@link AgentEvent} into an {@link SseEvent} the chat stream can
 * forward, and produces the terminal {@code done}/{@code error} events for the stream's
 * completion/error callbacks. Stateless and thread-safe.
 *
 * <p>Event vocabulary:</p>
 * <ul>
 *   <li>{@link #EVENT_TOKEN} — incremental assistant text delta</li>
 *   <li>{@link #EVENT_THINKING} — incremental reasoning delta (chain-of-thought)</li>
 *   <li>{@link #EVENT_DONE} — stream completed normally</li>
 *   <li>{@link #EVENT_ERROR} — stream terminated by an exception</li>
 * </ul>
 */
public final class SseEventMapper {

    public static final String EVENT_TOKEN = "token";
    public static final String EVENT_THINKING = "thinking";
    public static final String EVENT_DONE = "done";
    public static final String EVENT_ERROR = "error";

    private static final String DONE_DATA = "[DONE]";
    private static final String UNKNOWN_ERROR = "unknown error";

    /**
     * Map a stream event to an SSE event, or empty if the event type is not one the stream
     * surface cares about (e.g. tool-call lifecycle, message boundaries).
     */
    public Optional<SseEvent> map(AgentEvent event) {
        return switch (event) {
            case TextBlockDeltaEvent e -> Optional.of(new SseEvent(EVENT_TOKEN, e.getDelta()));
            case ThinkingBlockDeltaEvent e -> Optional.of(new SseEvent(EVENT_THINKING, e.getDelta()));
            case null, default -> Optional.empty();
        };
    }

    /** Terminal event emitted on stream completion. */
    public SseEvent doneEvent() {
        return new SseEvent(EVENT_DONE, DONE_DATA);
    }

    /** Terminal event emitted on stream error. Always non-null data. */
    public SseEvent errorEvent(Throwable t) {
        String message = (t == null || t.getMessage() == null || t.getMessage().isBlank())
                ? UNKNOWN_ERROR
                : t.getMessage();
        return new SseEvent(EVENT_ERROR, message);
    }
}
