package com.glodon.mordor.yansen.api.dto;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: HTTP request body for {@code POST /api/chat}. {@code prompt} is required;
 * {@code userId} and {@code sessionId} are server-assigned when absent.
 */
public record ChatRequest(String prompt, String userId, String sessionId) {
}
