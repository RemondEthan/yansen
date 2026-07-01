package com.glodon.mordor.yansen.config.store;

/**
 * @author: Remond
 * @date: 2026-07-01
 * @description: Persistence type for skill_config table.
 */
public record SkillConfigRecord(
        String skillId,
        String name,
        String sourceType,
        String sourceRef,
        String createdAt,
        String updatedAt) {
}
