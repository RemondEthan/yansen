package com.glodon.mordor.yansen.config.store;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Persistence type for system_prompt table.
 */
public record SystemPromptRecord(
        Integer id,
        String name,
        String sourceType,
        String sourceRef,
        String createdAt,
        String updatedAt) {
}
