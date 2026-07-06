package com.glodon.mordor.yansen.api.dto.config;

/**
 * @author: Remond
 * @date: 2026-07-06
 * @description: Request body for skill config create/update.
 */
public record SkillConfigRequest(
        String skillId,
        String name,
        String sourceType,
        String sourceRef) {
}
