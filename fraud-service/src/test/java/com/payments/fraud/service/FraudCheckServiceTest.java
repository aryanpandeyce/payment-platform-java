package com.payments.fraud.service;

import com.payments.fraud.config.FraudProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FraudCheckServiceTest {

    private FraudProperties properties;
    private FraudCheckService service;
    private final UUID senderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new FraudProperties();
        properties.setMaxAmount(new BigDecimal("50000.00"));
        properties.setMaxVelocityPerMinute(5);
        service = new FraudCheckService(properties);
    }

    @Test
    @DisplayName("FR-8: Amount <= ₹50,000.00 passes fraud rule")
    void shouldPassWhenAmountWithinLimit() {
        FraudCheckResult result = service.evaluate(senderId, new BigDecimal("50000.00"));
        assertTrue(result.isPassed());
        assertNull(result.getFailureReason());
    }

    @Test
    @DisplayName("FR-8: Amount > ₹50,000.00 fails with EXCEEDS_MAX_AMOUNT_LIMIT")
    void shouldFailWhenAmountExceedsLimit() {
        FraudCheckResult result = service.evaluate(senderId, new BigDecimal("50000.01"));
        assertFalse(result.isPassed());
        assertEquals("EXCEEDS_MAX_AMOUNT_LIMIT", result.getFailureReason());
    }

    @Test
    @DisplayName("FR-9: Up to 5 transfers per minute pass, 6th fails with VELOCITY_LIMIT_EXCEEDED")
    void shouldEnforceVelocityLimitPerMinute() {
        BigDecimal amount = new BigDecimal("100.00");

        // First 5 transfers pass
        for (int i = 1; i <= 5; i++) {
            FraudCheckResult result = service.evaluate(senderId, amount);
            assertTrue(result.isPassed(), "Transfer " + i + " should pass");
        }

        // 6th transfer in the same window fails
        FraudCheckResult sixthResult = service.evaluate(senderId, amount);
        assertFalse(sixthResult.isPassed());
        assertEquals("VELOCITY_LIMIT_EXCEEDED", sixthResult.getFailureReason());
    }
}
