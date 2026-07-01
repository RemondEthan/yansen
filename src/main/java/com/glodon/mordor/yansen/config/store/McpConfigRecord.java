package com.glodon.mordor.yansen.config.store;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Persistence type for mcp_config table. The {@code config} field holds
 * JSON-serialized McpServerConfig; deserialization is handled by McpRegistry.
 */
public record McpConfigRecord(
        String mcpId,
        String name,
        String config,
        String createdAt,
        String updatedAt) {
}
