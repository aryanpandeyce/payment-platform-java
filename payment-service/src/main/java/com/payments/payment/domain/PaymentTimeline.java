package com.payments.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

// PaymentTimeline: Har payment ke lifecycle transition ka audit record (INITIATED -> VALIDATED -> SETTLED/FAILED)
@Entity
@Table(name = "payment_timeline")
public class PaymentTimeline {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false)
    private PaymentStatus status;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "failure_reason", updatable = false)
    private String failureReason;

    protected PaymentTimeline() {
    }

    public PaymentTimeline(UUID paymentId, PaymentStatus status, Instant timestamp, String failureReason) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.status = status;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.failureReason = failureReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
