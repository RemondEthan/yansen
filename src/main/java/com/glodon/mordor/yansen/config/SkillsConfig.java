package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Where to discover SKILL.md repositories.
 * <ul>
 *   <li>{@code workspaceDir} — path relative to the agent workspace (e.g. "skills")</li>
 *   <li>{@code classpathBase} — classpath base to scan (e.g. "skills")</li>
 * </ul>
 * Either may be null/blank to disable that source.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillsConfig(String workspaceDir, String classpathBase) {
}
