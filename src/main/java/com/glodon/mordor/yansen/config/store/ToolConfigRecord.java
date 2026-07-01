package com.glodon.mordor.yansen.config.store;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Persistence type for tool_config table.
 */
public record ToolConfigRecord(
        String toolId,
        String name,
        String description,
        boolean enabled,
        String createdAt,
        String updatedAt) {
}
