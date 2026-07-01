package com.glodon.mordor.yansen.config.store;

import java.util.List;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Thrown when a config entity cannot be deleted because agents still reference it.
 * Mapped to HTTP 409 by {@link com.glodon.mordor.yansen.api.GlobalExceptionMapper}.
 */
public class ConfigReferenceException extends RuntimeException {

    private final String entityType;
    private final String entityId;
    private final List<String> referencingAgents;

    public ConfigReferenceException(String entityType, String entityId, List<String> referencingAgents) {
        super(buildMessage(entityType, entityId, referencingAgents));
        this.entityType = entityType;
        this.entityId = entityId;
        this.referencingAgents = List.copyOf(referencingAgents);
    }

    public String entityType() {
        return entityType;
    }

    public String entityId() {
        return entityId;
    }

    public List<String> referencingAgents() {
        return referencingAgents;
    }

    private static String buildMessage(String entityType, String entityId, List<String> referencingAgents) {
        return "Cannot delete " + entityType + " '" + entityId
                + "': referenced by agent(s) " + referencingAgents;
    }
}
