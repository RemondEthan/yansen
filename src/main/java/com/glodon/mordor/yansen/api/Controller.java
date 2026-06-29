package com.glodon.mordor.yansen.api;

import io.javalin.config.RoutesConfig;

/**
 * @author: Remond
 * @date: 2026-06-29
 * @description: Contract for API controllers that bind their routes onto a
 * {@link RoutesConfig}. The method name {@code registerOn} makes the direction
 * explicit: the controller registers <em>itself</em> <em>on</em> the given config.
 */
public interface Controller {

    void registerOn(RoutesConfig routes);
}
