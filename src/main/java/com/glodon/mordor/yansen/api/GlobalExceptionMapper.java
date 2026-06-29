package com.glodon.mordor.yansen.api;

import com.glodon.mordor.yansen.api.dto.ErrorResponse;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Catch-all error handler. Translates any unhandled exception into a uniform
 * {@link ErrorResponse} so the client never receives an HTML stack trace or empty body.
 * More specific handlers (e.g. 400 vs 500) can be layered in by adding more
 * {@code routes.exception(...)} calls.
 */
public final class GlobalExceptionMapper {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    private static final String CODE_INTERNAL = "internal_error";
    private static final String CODE_BAD_REQUEST = "bad_request";
    private static final String CODE_UPSTREAM_TIMEOUT = "upstream_timeout";

    private GlobalExceptionMapper() {
    }

    public static void register(RoutesConfig routes) {
        routes.exception(UpstreamTimeoutException.class, GlobalExceptionMapper::handleUpstreamTimeout);
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

    private static void handleUpstreamTimeout(UpstreamTimeoutException e, Context ctx) {
        log.error("Upstream timeout on {} {}: {}", ctx.method(), ctx.path(), e.getMessage());
        ctx.status(504).json(new ErrorResponse(CODE_UPSTREAM_TIMEOUT, safeMessage(e)));
    }

    private static String safeMessage(Throwable t) {
        return t == null || t.getMessage() == null ? t == null ? "" : t.getClass().getSimpleName() : t.getMessage();
    }
}
