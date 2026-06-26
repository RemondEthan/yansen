package com.glodon.mordor.yansen.api.dto;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: HTTP response body for {@code POST /api/chat}. {@code sessionId} is echoed
 * back (server-assigned if the request omitted one) so the client can reuse it on the stream
 * endpoint to keep the same conversation.
 */
public record ChatResponse(String response, String sessionId) {
}
