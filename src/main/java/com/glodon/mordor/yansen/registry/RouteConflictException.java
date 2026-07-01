package com.glodon.mordor.yansen.registry;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Thrown when registering a route that is already taken or reserved.
 */
public class RouteConflictException extends RuntimeException {

    public RouteConflictException(String message) {
        super(message);
    }
}
