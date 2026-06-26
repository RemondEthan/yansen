package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.api.dto.SseEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseEventMapperTest {

    private final SseEventMapper mapper = new SseEventMapper();

    @Test
    void textBlockDeltaMapsToTokenEvent() {
        AgentEvent event = stubTextDelta("Hello");
        Optional<SseEvent> sse = mapper.map(event);
        assertTrue(sse.isPresent());
        assertEquals(SseEventMapper.EVENT_TOKEN, sse.get().name());
        assertEquals("Hello", sse.get().data());
    }

    @Test
    void thinkingBlockDeltaMapsToThinkingEvent() {
        AgentEvent event = stubThinkingDelta("reasoning...");
        Optional<SseEvent> sse = mapper.map(event);
        assertTrue(sse.isPresent());
        assertEquals(SseEventMapper.EVENT_THINKING, sse.get().name());
        assertEquals("reasoning...", sse.get().data());
    }

    @Test
    void unrecognisedEventTypeMapsToEmpty() {
        AgentEvent event = new AgentEvent() {
            @Override public AgentEventType getType() { return AgentEventType.TOOL_CALL_START; }
        };
        assertFalse(mapper.map(event).isPresent());
    }

    @Test
    void nullEventMapsToEmpty() {
        assertFalse(mapper.map(null).isPresent());
    }

    @Test
    void doneEventIsStable() {
        SseEvent done = mapper.doneEvent();
        assertNotNull(done);
        assertEquals(SseEventMapper.EVENT_DONE, done.name());
        assertEquals("[DONE]", done.data());
    }

    @Test
    void errorEventCarriesMessage() {
        SseEvent err = mapper.errorEvent(new RuntimeException("boom"));
        assertEquals(SseEventMapper.EVENT_ERROR, err.name());
        assertEquals("boom", err.data());
    }

    @Test
    void errorEventFallsBackWhenMessageIsNull() {
        SseEvent err = mapper.errorEvent(new RuntimeException());
        assertEquals("unknown error", err.data());
    }

    @Test
    void errorEventFallsBackWhenMessageIsBlank() {
        SseEvent err = mapper.errorEvent(new RuntimeException("   "));
        assertEquals("unknown error", err.data());
    }

    @Test
    void errorEventFallsBackWhenThrowableIsNull() {
        SseEvent err = mapper.errorEvent(null);
        assertEquals("unknown error", err.data());
    }

    @Test
    void doneEventFreshInstancePerCall() {
        // Stream produces many done events over its lifetime; mapper must not reuse one.
        SseEvent a = mapper.doneEvent();
        SseEvent b = mapper.doneEvent();
        assertEquals(a, b);
        assertNotNull(a);
    }

    private static AgentEvent stubTextDelta(String delta) {
        return new TextBlockDeltaEvent(null, null, delta);
    }

    private static AgentEvent stubThinkingDelta(String delta) {
        return new ThinkingBlockDeltaEvent(null, null, delta);
    }
}
