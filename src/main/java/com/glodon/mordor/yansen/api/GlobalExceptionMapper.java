package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.agent.AgentNotFoundException;
import com.glodon.mordor.yansen.api.dto.ErrorResponse;
import com.glodon.mordor.yansen.config.store.ConfigReferenceException;
import com.glodon.mordor.yansen.registry.RouteConflictException;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Catch-all error handler. Translates unhandled exceptions into {@link ErrorResponse}.
 */
public final class GlobalExceptionMapper {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    private static final String CODE_INTERNAL = "internal_error";
    private static final String CODE_BAD_REQUEST = "bad_request";
    private static final String CODE_NOT_FOUND = "not_found";
    private static final String CODE_CONFLICT = "conflict";
    private static final String CODE_UPSTREAM_TIMEOUT = "upstream_timeout";

    private GlobalExceptionMapper() {
    }

    public static void register(RoutesConfig routes) {
        routes.exception(UpstreamTimeoutException.class, GlobalExceptionMapper::handleUpstreamTimeout);
        routes.exception(RouteNotFoundException.class, GlobalExceptionMapper::handleNotFound);
        routes.exception(AgentNotFoundException.class, GlobalExceptionMapper::handleBadRequestAgent);
        routes.exception(ConfigReferenceException.class, GlobalExceptionMapper::handleConflict);
        routes.exception(RouteConflictException.class, GlobalExceptionMapper::handleConflictRoute);
        routes.exception(IllegalArgumentException.class, GlobalExceptionMapper::handleBadRequest);
        routes.exception(Exception.class, GlobalExceptionMapper::handleInternal);
    }

    private static void handleInternal(Exception e, Context ctx) {
        log.error("Unhandled exception on {} {}", ctx.method(), ctx.path(), e);
        ctx.status(500).json(new ErrorResponse(CODE_INTERNAL, safeMessage(e)));
    }

    private static void handleBadRequest(IllegalArgumentException e, Context ctx) {
        log.warn("Bad request on {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
        ctx.status(400).json(new ErrorResponse(CODE_BAD_REQUEST, safeMessage(e)));
    }

    private static void handleBadRequestAgent(AgentNotFoundException e, Context ctx) {
        log.warn("Agent not found on {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
        ctx.status(400).json(new ErrorResponse(CODE_BAD_REQUEST, safeMessage(e)));
    }

    private static void handleNotFound(RouteNotFoundException e, Context ctx) {
        log.warn("Not found on {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
        ctx.status(404).json(new ErrorResponse(CODE_NOT_FOUND, safeMessage(e)));
    }

    private static void handleConflict(ConfigReferenceException e, Context ctx) {
        log.warn("Conflict on {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
        ctx.status(409).json(new ErrorResponse(CODE_CONFLICT, safeMessage(e)));
    }

    private static void handleConflictRoute(RouteConflictException e, Context ctx) {
        log.warn("Route conflict on {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
        ctx.status(409).json(new ErrorResponse(CODE_CONFLICT, safeMessage(e)));
    }

    private static void handleUpstreamTimeout(UpstreamTimeoutException e, Context ctx) {
        log.error("Upstream timeout on {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
        ctx.status(504).json(new ErrorResponse(CODE_UPSTREAM_TIMEOUT, safeMessage(e)));
    }

    private static String safeMessage(Throwable t) {
        return t == null || t.getMessage() == null ? t == null ? "" : t.getClass().getSimpleName() : t.getMessage();
    }
}
