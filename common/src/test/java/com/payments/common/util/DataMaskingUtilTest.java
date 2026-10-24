package com.payments.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataMaskingUtilTest {

    @Test
    @DisplayName("Should mask UUID account ID to preserve only last 4 digits")
    void shouldMaskAccountUuid() {
        String accountId = "11111111-1111-1111-1111-111111111101";
        String masked = DataMaskingUtil.maskAccountId(accountId);
        assertEquals("****-1101", masked);
    }

    @Test
    @DisplayName("Should mask password fields in JSON string")
    void shouldMaskPasswordInJson() {
        String json = "{\"username\":\"rahul.sharma\",\"password\":\"SecretPass123!\",\"email\":\"rahul@example.com\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("SecretPass123!"));
        assertTrue(masked.contains("\"password\":\"[PROTECTED]\""));
        assertTrue(masked.contains("\"username\":\"rahul.sharma\""));
    }

    @Test
    @DisplayName("Should mask Authorization Bearer token")
    void shouldMaskBearerToken() {
        String authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.sflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String masked = DataMaskingUtil.maskToken(authHeader);
        assertFalse(masked.contains("sflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
        assertTrue(masked.startsWith("Bearer eyJhbGciOiJIUzI1...[PROTECTED]"));
    }

    @Test
    @DisplayName("Should mask full log message containing mixed sensitive credentials")
    void shouldMaskSensitiveLogMessage() {
        String rawLog = "Processing transfer for account 11111111-1111-1111-1111-111111111101 with password=SuperSecretPassword and token=abc123xyz789";
        String sanitized = DataMaskingUtil.maskLogMessage(rawLog);
        assertFalse(sanitized.contains("SuperSecretPassword"));
        assertFalse(sanitized.contains("abc123xyz789"));
        assertTrue(sanitized.contains("password=******"));
        assertTrue(sanitized.contains("token=******"));
    }
}
