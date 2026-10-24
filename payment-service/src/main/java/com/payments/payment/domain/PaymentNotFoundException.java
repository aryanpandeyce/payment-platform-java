package com.payments.payment.domain;

import java.util.UUID;

// Payment ID na milne par throw hone wala exception (HTTP 404)
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID id) {
        super("Payment not found with ID: " + id);
    }
}
