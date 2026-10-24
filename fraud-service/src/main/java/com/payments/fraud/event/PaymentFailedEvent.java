package com.payments.fraud.event;

import java.time.Instant;
import java.util.UUID;

// Fraud validation fail hone par Kafka topic 'payment-failed' par publish hone wala event
public class PaymentFailedEvent {

    private UUID paymentId;
    private String failureReason;
    private Instant timestamp;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(UUID paymentId, String failureReason) {
        this.paymentId = paymentId;
        this.failureReason = failureReason;
        this.timestamp = Instant.now();
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
