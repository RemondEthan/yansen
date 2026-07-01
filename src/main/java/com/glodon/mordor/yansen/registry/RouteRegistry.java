package com.glodon.mordor.yansen.registry;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Maps HTTP route paths to agent ids. Supports mark-invalid for deleted agents.
 */
public final class RouteRegistry {

    private static final String STREAM_SUFFIX = "/stream";

    private static final Set<String> RESERVED_ROUTES = Set.of(
            "/api/health");

    private static final Set<String> RESERVED_PREFIXES = Set.of(
            "/api/config");

    private final ConcurrentHashMap<String, String> activeRoutes = new ConcurrentHashMap<>();
    private final Set<String> disabledRoutes = ConcurrentHashMap.newKeySet();

    public void register(String route, String agentId) {
        validateRoute(route);
        String existing = activeRoutes.get(route);
        if (existing != null && !existing.equals(agentId)) {
            throw new RouteConflictException(
                    "route '" + route + "' is already registered to agent '" + existing + "'");
        }
        disabledRoutes.remove(route);
        activeRoutes.put(route, agentId);
    }

    public void unregister(String route) {
        activeRoutes.remove(route);
        disabledRoutes.add(route);
    }

    /**
     * Resolves a request path to an agent id. Accepts both {@code /api/chat} and
     * {@code /api/chat/stream}.
     */
    public Optional<String> resolveRequestPath(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return Optional.empty();
        }
        String baseRoute = requestPath.endsWith(STREAM_SUFFIX)
                ? requestPath.substring(0, requestPath.length() - STREAM_SUFFIX.length())
                : requestPath;
        return resolve(baseRoute);
    }

    public Optional<String> resolve(String route) {
        if (disabledRoutes.contains(route)) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeRoutes.get(route));
    }

    public boolean isActive(String route) {
        return resolve(route).isPresent();
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(activeRoutes);
    }

    public void validateRoute(String route) {
        if (route == null || route.isBlank()) {
            throw new IllegalArgumentException("route must not be blank");
        }
        if (!route.startsWith("/")) {
            throw new IllegalArgumentException("route must start with '/': " + route);
        }
        if (route.endsWith(STREAM_SUFFIX)) {
            throw new IllegalArgumentException("route must not end with '/stream': " + route);
        }
        if (RESERVED_ROUTES.contains(route)) {
            throw new RouteConflictException("route '" + route + "' conflicts with a reserved system route");
        }
        for (String prefix : RESERVED_PREFIXES) {
            if (route.equals(prefix) || route.startsWith(prefix + "/")) {
                throw new RouteConflictException(
                        "route '" + route + "' conflicts with reserved prefix '" + prefix + "'");
            }
        }
    }
}
