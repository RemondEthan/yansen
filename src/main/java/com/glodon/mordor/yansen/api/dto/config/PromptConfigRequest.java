package com.glodon.mordor.yansen.api.dto.config;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Request body for system prompt create/update.
 */
public record PromptConfigRequest(
        String name,
        String sourceType,
        String sourceRef) {
}
