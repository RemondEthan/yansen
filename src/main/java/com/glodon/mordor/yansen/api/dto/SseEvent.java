package com.glodon.mordor.yansen.api.dto;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: A named Server-Sent Event. {@code name} becomes the {@code event:} line;
 * {@code data} becomes the {@code data:} line. Both are sent as raw bytes (no JSON wrapping),
 * so {@code data} should be a plain string unless the client expects encoded JSON.
 */
public record SseEvent(String name, String data) {
}
