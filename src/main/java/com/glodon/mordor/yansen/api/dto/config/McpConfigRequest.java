package com.glodon.mordor.yansen.api.dto.config;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Request body for MCP config create/update.
 */
public record McpConfigRequest(
        String mcpId,
        String name,
        String config) {
}
