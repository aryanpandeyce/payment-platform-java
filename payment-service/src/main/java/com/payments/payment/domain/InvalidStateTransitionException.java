package com.payments.payment.domain;

// Illegal state transition hone par throw hone wala exception (e.g. SETTLED se FAILED)
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Invalid payment state transition from " + from + " to " + to);
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
