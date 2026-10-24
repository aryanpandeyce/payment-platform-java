package com.payments.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Standard error response envelope across all ApexPay microservices.
 *
 * @param timestamp Time error occurred
 * @param status HTTP numeric status code
 * @param code High-level business error code (e.g. INSUFFICIENT_FUNDS, FORBIDDEN)
 * @param errorCode Backward-compatible alias for code
 * @param message Human readable detail message
 * @param path Request path
 * @param correlationId Distributed tracing correlation ID
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    @JsonProperty("errorCode")
    String errorCode,
    String message,
    String path,
    String correlationId
) {
    public ErrorResponse(Instant timestamp, int status, String code, String message, String path, String correlationId) {
        this(timestamp, status, code, code, message, path, correlationId);
    }

    public ErrorResponse(int status, String code, String message, String path, String correlationId) {
        this(Instant.now(), status, code, code, message, path, correlationId);
    }

    public ErrorResponse(int status, String code, String message, String path) {
        this(Instant.now(), status, code, code, message, path, null);
    }

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, code, message, path, null);
    }

    public static ErrorResponse of(int status, String code, String message, String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, code, code, message, path, correlationId);
    }
}
