package com.payments.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        @JsonProperty("errorCode")
        String errorCode,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, code, message, path);
    }
}
