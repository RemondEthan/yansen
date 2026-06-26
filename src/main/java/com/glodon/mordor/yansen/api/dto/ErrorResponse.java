package com.glodon.mordor.yansen.api.dto;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Standard error envelope. {@code error} is a short machine-friendly code
 * (e.g. "internal_error", "bad_request"); {@code message} is the human-readable detail.
 */
public record ErrorResponse(String error, String message) {
}
