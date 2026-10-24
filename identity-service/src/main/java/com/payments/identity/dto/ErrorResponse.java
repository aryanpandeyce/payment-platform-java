package com.payments.identity.dto;

import java.time.Instant;

// Standard Platform Error Response Format
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String errorCode,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(Instant.now(), status, errorCode, errorCode, message, path);
    }
}
