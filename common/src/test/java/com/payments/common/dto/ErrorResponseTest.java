package com.payments.common.dto;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void shouldConstructErrorResponseWithTimestamp() {
        ErrorResponse err = new ErrorResponse(404, "NOT_FOUND", "Account not found", "/api/accounts/1", "corr-1");

        assertNotNull(err.timestamp());
        assertEquals(404, err.status());
        assertEquals("NOT_FOUND", err.code());
        assertEquals("Account not found", err.message());
        assertEquals("/api/accounts/1", err.path());
        assertEquals("corr-1", err.correlationId());
    }

    @Test
    void shouldConstructWithExplicitTimestamp() {
        Instant now = Instant.parse("2026-10-24T12:00:00Z");
        ErrorResponse err = new ErrorResponse(now, 403, "FORBIDDEN", "Forbidden access", "/api/accounts", "corr-2");

        assertEquals(now, err.timestamp());
        assertEquals(403, err.status());
        assertEquals("FORBIDDEN", err.code());
    }
}
