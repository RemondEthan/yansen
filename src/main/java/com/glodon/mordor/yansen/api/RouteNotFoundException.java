package com.glodon.mordor.yansen.api;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Thrown when no active agent route matches the request path.
 */
public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String message) {
        super(message);
    }
}
