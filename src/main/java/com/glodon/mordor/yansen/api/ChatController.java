package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.api.dto.ChatRequest;
import com.glodon.mordor.yansen.api.dto.ChatResponse;
import com.glodon.mordor.yansen.api.dto.SseEvent;
import com.glodon.mordor.yansen.service.YansenAgentService;
import io.agentscope.core.event.AgentEvent;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.UUID;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: HTTP surface for the agent service: synchronous {@code POST /api/chat} and
 * streaming {@code GET /api/chat/stream} (SSE). All SSE event translation goes through
 * {@link SseEventMapper}; this class is concerned with HTTP/SSE wiring only.
 */
public final class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String ROUTE_CHAT = "/api/chat";
    private static final String ROUTE_CHAT_STREAM = "/api/chat/stream";

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String QUERY_PROMPT = "prompt";
    private static final String QUERY_USER_ID = "userId";
    private static final String QUERY_SESSION_ID = "sessionId";
    private static final String DEFAULT_USER_ID = "default-user";
    private static final String SSE_TERMINAL_NAME = "error";
    private static final String SSE_MISSING_PROMPT = "prompt is required";

    /**
     * Inter-event idle threshold that warrants a WARN log. Kept well below the configured
     * SSE idle timeout (default 300s) so long thinking/CoT pauses that could cut the
     * connection are surfaced while debugging.
     */
    private static final long LARGE_EVENT_GAP_MS = 5_000L;

    private final YansenAgentService agentService;
    private final SseEventMapper sseMapper;
    private final ScheduledExecutorService heartbeatExecutor;
    private final long keepAliveIntervalSeconds;

    public ChatController(YansenAgentService agentService, SseEventMapper sseMapper,
                          ScheduledExecutorService heartbeatExecutor, long keepAliveIntervalSeconds) {
        this.agentService = agentService;
        this.sseMapper = sseMapper;
        this.heartbeatExecutor = heartbeatExecutor;
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
    }

    public void register(RoutesConfig routes) {
        routes.apiBuilder((EndpointGroup) () -> {
            ApiBuilder.post(ROUTE_CHAT, this::handleChat);
            ApiBuilder.sse(ROUTE_CHAT_STREAM, this::handleChatStream);
        });
    }

    private void handleChat(Context ctx) {
        ctx.contentType(CONTENT_TYPE_JSON);
        ChatRequest req = ctx.bodyAsClass(ChatRequest.class);
        String sessionId = req.sessionId() != null ? req.sessionId() : UUID.randomUUID().toString();
        String userId = req.userId() != null ? req.userId() : DEFAULT_USER_ID;

        String response = agentService.chat(req.prompt(), userId, sessionId);
        ctx.json(new ChatResponse(response, sessionId));
    }

    private void handleChatStream(SseClient sseClient) {
        Context ctx = sseClient.ctx();
        String prompt = ctx.queryParam(QUERY_PROMPT);
        if (prompt == null || prompt.isBlank()) {
            safeSendEvent(sseClient, SSE_TERMINAL_NAME, SSE_MISSING_PROMPT);
            sseClient.close();
            return;
        }
        String userId = ctx.queryParam(QUERY_USER_ID);
        String sessionId = ctx.queryParam(QUERY_SESSION_ID);
        if (userId == null) {
            userId = DEFAULT_USER_ID;
        }
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        final String effectiveSessionId = sessionId;

        log.info("[SSE] open prompt=\"{}\" userId={} sessionId={}", prompt, userId, effectiveSessionId);

        final long streamStartMs = System.currentTimeMillis();
        final AtomicLong lastEventMs = new AtomicLong(streamStartMs);
        final AtomicLong eventCount = new AtomicLong(0);
        // Set by the Flux completion / error paths; if neither is true when the SseClient
        // closes, the connection was cut externally (Jetty idle timeout or client disconnect).
        final AtomicBoolean completedByApp = new AtomicBoolean(false);
        // Block the Javalin async context until sseClient.close() is called.
        // Without this, the async task completes as soon as handleChatStream returns
        // (which is immediately after subscribe), and Jetty closes the connection
        // before the Flux has a chance to emit any events.
        sseClient.keepAlive();

        // Heartbeat keeps the idle timeout from cutting the stream during long
        // thinking/CoT silences. Scheduled after keepAlive so the async context is
        // already blocked — no window where Jetty could close the connection.
        final ScheduledFuture<?> heartbeat = scheduleHeartbeat(sseClient, effectiveSessionId);
        log.debug("[SSE] stream ready sessionId={} (awaiting events)", effectiveSessionId);

        Disposable disposable = agentService.chatStream(prompt, userId, effectiveSessionId)
                .doOnNext(event -> {
                    long nowMs = System.currentTimeMillis();
                    long gapMs = nowMs - lastEventMs.getAndSet(nowMs);
                    long elapsedMs = nowMs - streamStartMs;
                    long count = eventCount.incrementAndGet();
                    log.debug("[SSE] event sessionId={} #{} type={} class={} elapsed={}ms gap={}ms",
                            effectiveSessionId, count, event.getType(),
                            event.getClass().getSimpleName(), elapsedMs, gapMs);
                    if (gapMs > LARGE_EVENT_GAP_MS) {
                        log.warn("[SSE] large event gap sessionId={} gap={}ms",
                                effectiveSessionId, gapMs);
                    }
                })
                .subscribe(
                        event -> forward(sseClient, event),
                        error -> {
                            completedByApp.set(true);
                            cancelHeartbeat(heartbeat);
                            terminateWithError(sseClient, effectiveSessionId, error,
                                    eventCount.get(), streamStartMs);
                        },
                        () -> {
                            completedByApp.set(true);
                            cancelHeartbeat(heartbeat);
                            terminateWithDone(sseClient, effectiveSessionId,
                                    eventCount.get(), streamStartMs);
                        });

        sseClient.onClose(() -> {
            // Catch-all: covers external cuts (Jetty idle timeout / client disconnect) where
            // neither terminateWith* ran. Idempotent against the cancels above.
            cancelHeartbeat(heartbeat);
            long elapsedMs = System.currentTimeMillis() - streamStartMs;
            boolean appInitiated = completedByApp.get();
            if (appInitiated) {
                log.debug("[SSE] connection closed sessionId={} events={} elapsed={}ms",
                        effectiveSessionId, eventCount.get(), elapsedMs);
            } else {
                log.warn("[SSE] connection closed WITHOUT app completion sessionId={} "
                                + "events={} elapsed={}ms \u2014 idle timeout or client "
                                + "disconnect cut the stream before the Flux finished",
                        effectiveSessionId, eventCount.get(), elapsedMs);
            }
            disposable.dispose();
        });
    }

    /**
     * Schedules a recurring SSE comment on the shared executor. The comment is a no-op for
     * clients (Insomnia, browsers) but counts as bytes on the wire, so Jetty's idle timeout
     * never fires during long thinking/CoT silences. Returns {@code null} when heartbeats
     * are disabled, so callers can no-op the cancel.
     */
    private ScheduledFuture<?> scheduleHeartbeat(SseClient sseClient, String sessionId) {
        if (keepAliveIntervalSeconds <= 0L) {
            log.debug("[SSE] heartbeat disabled sessionId={} (interval=0)", sessionId);
            return null;
        }
        ScheduledFuture<?> future = heartbeatExecutor.scheduleAtFixedRate(
                () -> runHeartbeat(sseClient, sessionId),
                keepAliveIntervalSeconds, keepAliveIntervalSeconds, TimeUnit.SECONDS);
        log.debug("[SSE] heartbeat scheduled sessionId={} interval={}s",
                sessionId, keepAliveIntervalSeconds);
        return future;
    }

    private void runHeartbeat(SseClient sseClient, String sessionId) {
        try {
            safeSendComment(sseClient, "keepalive");
        } catch (RuntimeException e) {
            // Stream may have been closed between scheduling and firing; safe to ignore.
            log.debug("[SSE] heartbeat skipped sessionId={} reason={}",
                    sessionId, e.getMessage());
        }
    }

    private void cancelHeartbeat(ScheduledFuture<?> heartbeat) {
        if (heartbeat != null) {
            // false = don't interrupt an in-flight write; the next tick simply won't fire.
            heartbeat.cancel(false);
        }
    }

    /**
     * Synchronizes an {@link SseClient} write. The Javalin SSE output stream is not
     * thread-safe and is written to from both the Reactor thread (event forwarding /
     * terminal events) and the shared heartbeat executor. Locking on the client instance
     * keeps the two writers serial without introducing a separate lock object.
     *
     * <p>After each write we call {@code ctx().res().flushBuffer()}: Javalin's
     * {@link io.javalin.http.sse.Emitter} writes via {@code response.getOutputStream().print},
     * which never flushes, and Jetty's {@code HttpOutput} buffers small writes in an
     * aggregate (default 32KB) that only ships when full or on close. For SSE that means
     * bytes can sit in Jetty for tens of seconds while the TCP socket sees nothing —
     * Jetty's idle timeout then cuts the connection from the server side. Flushing each
     * event/comment pushes the current chunk onto the wire so the idle timeout resets and
     * the client sees tokens in real time.
     */
    private void safeSendEvent(SseClient sseClient, String name, String data) {
        synchronized (sseClient) {
            sseClient.sendEvent(name, data);
            flushResponse(sseClient);
        }
    }

    private void safeSendComment(SseClient sseClient, String text) {
        synchronized (sseClient) {
            sseClient.sendComment(text);
            flushResponse(sseClient);
        }
    }

    private void flushResponse(SseClient sseClient) {
        try {
            sseClient.ctx().res().flushBuffer();
        } catch (IOException e) {
            // Most likely the client disconnected; the next send* call will surface it
            // to the onClose path. Don't let a transient flush failure tear down the stream.
            log.debug("[SSE] flush failed: {}", e.getMessage());
        }
    }

    private void forward(SseClient sseClient, AgentEvent event) {
        sseMapper.map(event).ifPresent(sse -> safeSendEvent(sseClient, sse.name(), sse.data()));
    }

    private void terminateWithDone(SseClient sseClient, String sessionId,
                                   long eventCount, long streamStartMs) {
        SseEvent done = sseMapper.doneEvent();
        safeSendEvent(sseClient, done.name(), done.data());
        sseClient.close();
        log.info("[SSE] close sessionId={} reason=complete events={} elapsed={}ms",
                sessionId, eventCount, System.currentTimeMillis() - streamStartMs);
    }

    private void terminateWithError(SseClient sseClient, String sessionId, Throwable error,
                                    long eventCount, long streamStartMs) {
        log.error("[SSE] close sessionId={} reason=error events={} elapsed={}ms",
                sessionId, eventCount, System.currentTimeMillis() - streamStartMs, error);
        SseEvent err = sseMapper.errorEvent(error);
        safeSendEvent(sseClient, err.name(), err.data());
        sseClient.close();
    }
}
