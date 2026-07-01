package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.api.dto.SseEvent;
import io.agentscope.core.event.AgentEvent;
import io.javalin.http.sse.SseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Encapsulates SSE stream lifecycle: heartbeat, Flux subscription, synchronized writes.
 */
final class SseSession {

    private static final Logger log = LoggerFactory.getLogger(SseSession.class);
    private static final long LARGE_EVENT_GAP_MS = 5_000L;

    private final SseClient sseClient;
    private final SseEventMapper sseMapper;
    private final ScheduledExecutorService heartbeatExecutor;
    private final long keepAliveIntervalSeconds;
    private final long sseEventTimeoutSeconds;
    private final String sessionId;

    private final long streamStartMs = System.currentTimeMillis();
    private final AtomicLong lastEventMs = new AtomicLong(streamStartMs);
    private final AtomicLong eventCount = new AtomicLong(0);
    private final AtomicBoolean completedByApp = new AtomicBoolean(false);

    SseSession(SseClient sseClient, SseEventMapper sseMapper,
               ScheduledExecutorService heartbeatExecutor,
               long keepAliveIntervalSeconds, long sseEventTimeoutSeconds, String sessionId) {
        this.sseClient = sseClient;
        this.sseMapper = sseMapper;
        this.heartbeatExecutor = heartbeatExecutor;
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
        this.sseEventTimeoutSeconds = sseEventTimeoutSeconds;
        this.sessionId = sessionId;
    }

    void stream(Flux<AgentEvent> events) {
        sseClient.keepAlive();
        ScheduledFuture<?> heartbeat = scheduleHeartbeat();

        Disposable disposable = events
                .timeout(java.time.Duration.ofSeconds(sseEventTimeoutSeconds))
                .doOnNext(this::logEvent)
                .subscribe(
                        this::forward,
                        error -> {
                            completedByApp.set(true);
                            cancelHeartbeat(heartbeat);
                            terminateWithError(error);
                        },
                        () -> {
                            completedByApp.set(true);
                            cancelHeartbeat(heartbeat);
                            terminateWithDone();
                        });

        sseClient.onClose(() -> {
            cancelHeartbeat(heartbeat);
            long elapsedMs = System.currentTimeMillis() - streamStartMs;
            if (completedByApp.get()) {
                log.debug("[SSE] connection closed sessionId={} events={} elapsed={}ms",
                        sessionId, eventCount.get(), elapsedMs);
            } else {
                log.warn("[SSE] connection closed WITHOUT app completion sessionId={} "
                                + "events={} elapsed={}ms",
                        sessionId, eventCount.get(), elapsedMs);
            }
            disposable.dispose();
        });
    }

    void sendTerminalError(String message) {
        safeSendEvent(SseEventMapper.EVENT_ERROR, message);
        sseClient.close();
    }

    private void logEvent(AgentEvent event) {
        long nowMs = System.currentTimeMillis();
        long gapMs = nowMs - lastEventMs.getAndSet(nowMs);
        long count = eventCount.incrementAndGet();
        log.debug("[SSE] event sessionId={} #{} type={} class={} elapsed={}ms gap={}ms",
                sessionId, count, event.getType(), event.getClass().getSimpleName(),
                nowMs - streamStartMs, gapMs);
        if (gapMs > LARGE_EVENT_GAP_MS) {
            log.warn("[SSE] large event gap sessionId={} gap={}ms", sessionId, gapMs);
        }
    }

    private ScheduledFuture<?> scheduleHeartbeat() {
        if (keepAliveIntervalSeconds <= 0L) {
            return null;
        }
        return heartbeatExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        safeSendComment("keepalive");
                    } catch (RuntimeException e) {
                        log.debug("[SSE] heartbeat skipped sessionId={} reason={}",
                                sessionId, e.getMessage());
                    }
                },
                keepAliveIntervalSeconds, keepAliveIntervalSeconds, TimeUnit.SECONDS);
    }

    private void cancelHeartbeat(ScheduledFuture<?> heartbeat) {
        if (heartbeat != null) {
            heartbeat.cancel(false);
        }
    }

    private void forward(AgentEvent event) {
        sseMapper.map(event).ifPresent(sse -> safeSendEvent(sse.name(), sse.data()));
    }

    private void terminateWithDone() {
        SseEvent done = sseMapper.doneEvent();
        safeSendEvent(done.name(), done.data());
        sseClient.close();
        log.info("[SSE] close sessionId={} reason=complete events={} elapsed={}ms",
                sessionId, eventCount.get(), System.currentTimeMillis() - streamStartMs);
    }

    private void terminateWithError(Throwable error) {
        log.error("[SSE] close sessionId={} reason=error events={} elapsed={}ms",
                sessionId, eventCount.get(), System.currentTimeMillis() - streamStartMs, error);
        SseEvent err = sseMapper.errorEvent(error);
        safeSendEvent(err.name(), err.data());
        sseClient.close();
    }

    private void safeSendEvent(String name, String data) {
        synchronized (sseClient) {
            try {
                sseClient.sendEvent(name, data);
            } catch (RuntimeException e) {
                log.debug("[SSE] sendEvent failed: {}", e.getMessage());
            }
        }
    }

    private void safeSendComment(String text) {
        synchronized (sseClient) {
            try {
                sseClient.sendComment(text);
            } catch (RuntimeException e) {
                log.debug("[SSE] sendComment failed: {}", e.getMessage());
            }
        }
    }
}
