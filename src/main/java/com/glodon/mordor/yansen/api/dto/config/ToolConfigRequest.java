package com.glodon.mordor.yansen.api.dto.config;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Request body for tool config create/update.
 */
public record ToolConfigRequest(
        String toolId,
        String name,
        String description,
        Boolean enabled) {
}
