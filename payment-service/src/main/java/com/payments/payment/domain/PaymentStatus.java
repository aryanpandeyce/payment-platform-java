package com.payments.payment.domain;

// Payment lifecycle ke 4 official states: INITIATED -> VALIDATED -> SETTLED / FAILED
public enum PaymentStatus {
    INITIATED,
    VALIDATED,
    SETTLED,
    FAILED
}
