package com.payments.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Domain Unit Test: State Machine aur Financial Invariants verify karta hai
class PaymentTest {

    private final UUID senderId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();

    @Test
    @DisplayName("New payment starts in INITIATED status with scale 2 amount")
    void shouldCreatePaymentInInitiatedStatus() {
        Payment payment = new Payment("idemp-1", senderId, receiverId, new BigDecimal("500"), "INR");

        assertNotNull(payment.getId());
        assertEquals("idemp-1", payment.getIdempotencyKey());
        assertEquals(PaymentStatus.INITIATED, payment.getStatus());
        assertEquals(new BigDecimal("500.00"), payment.getAmount());
        assertNull(payment.getFailureReason());
    }

    @Test
    @DisplayName("State machine transitions from INITIATED to VALIDATED to SETTLED")
    void shouldFollowValidForwardHappyPathTransitions() {
        Payment payment = new Payment("idemp-2", senderId, receiverId, new BigDecimal("500.00"), "INR");

        payment.validate();
        assertEquals(PaymentStatus.VALIDATED, payment.getStatus());

        payment.settle();
        assertEquals(PaymentStatus.SETTLED, payment.getStatus());
    }

    @Test
    @DisplayName("State machine transitions to FAILED from INITIATED or VALIDATED")
    void shouldTransitionToFailedWithReason() {
        Payment payment1 = new Payment("idemp-3", senderId, receiverId, new BigDecimal("500.00"), "INR");
        payment1.fail("FRAUD_CHECK_FAILED");
        assertEquals(PaymentStatus.FAILED, payment1.getStatus());
        assertEquals("FRAUD_CHECK_FAILED", payment1.getFailureReason());

        Payment payment2 = new Payment("idemp-4", senderId, receiverId, new BigDecimal("500.00"), "INR");
        payment2.validate();
        payment2.fail("INSUFFICIENT_FUNDS");
        assertEquals(PaymentStatus.FAILED, payment2.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", payment2.getFailureReason());
    }

    @Test
    @DisplayName("Terminal states SETTLED and FAILED reject any further transitions")
    void shouldRejectTransitionsFromTerminalStates() {
        Payment payment = new Payment("idemp-5", senderId, receiverId, new BigDecimal("500.00"), "INR");
        payment.validate();
        payment.settle();

        // SETTLED -> FAILED is rejected
        assertThrows(InvalidStateTransitionException.class, () -> payment.fail("TRY_FAIL"));
        // SETTLED -> VALIDATED is rejected
        assertThrows(InvalidStateTransitionException.class, payment::validate);

        Payment failedPayment = new Payment("idemp-6", senderId, receiverId, new BigDecimal("500.00"), "INR");
        failedPayment.fail("REASON");

        // FAILED -> SETTLED is rejected
        assertThrows(InvalidStateTransitionException.class, failedPayment::settle);
        // FAILED -> VALIDATED is rejected
        assertThrows(InvalidStateTransitionException.class, failedPayment::validate);
    }

    @Test
    @DisplayName("Cannot skip VALIDATED state directly to SETTLED")
    void shouldRejectSkippingValidatedState() {
        Payment payment = new Payment("idemp-7", senderId, receiverId, new BigDecimal("500.00"), "INR");

        assertThrows(InvalidStateTransitionException.class, payment::settle);
    }

    @Test
    @DisplayName("Financial invariants: rejects self-transfer and non-positive amount")
    void shouldEnforceFinancialInvariants() {
        // Self transfer
        assertThrows(IllegalArgumentException.class, () ->
                new Payment("idemp-8", senderId, senderId, new BigDecimal("100.00"), "INR"));

        // Zero amount
        assertThrows(IllegalArgumentException.class, () ->
                new Payment("idemp-9", senderId, receiverId, BigDecimal.ZERO, "INR"));

        // Negative amount
        assertThrows(IllegalArgumentException.class, () ->
                new Payment("idemp-10", senderId, receiverId, new BigDecimal("-10.00"), "INR"));

        // Blank idempotency key
        assertThrows(IllegalArgumentException.class, () ->
                new Payment("", senderId, receiverId, new BigDecimal("10.00"), "INR"));
    }
}
