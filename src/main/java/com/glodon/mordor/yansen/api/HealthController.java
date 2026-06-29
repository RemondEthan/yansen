package com.glodon.mordor.yansen.api;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.RoutesConfig;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Liveness/readiness probe at {@code GET /api/health}. Returns 200 with a
 * constant payload; intentionally does not touch the agent service so it stays cheap and
 * never blocks.
 */
public final class HealthController implements Controller {

    private static final String ROUTE = "/api/health";

    @Override
    public void registerOn(RoutesConfig routes) {
        routes.apiBuilder((EndpointGroup) () -> ApiBuilder.get(ROUTE, ctx -> ctx.json(new HealthBody("ok"))));
    }

    public record HealthBody(String status) {
    }
}
