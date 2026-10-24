package com.payments.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

// @Entity: Hibernate is class ko database table "payments" se map karega
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true)
})
public class Payment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // UNIQUE column: Duplicate payment initiation prevent karne ke liye
    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(name = "sender_account_id", nullable = false, updatable = false)
    private UUID senderAccountId;

    @Column(name = "receiver_account_id", nullable = false, updatable = false)
    private UUID receiverAccountId;

    // Fixed scale 2 with RoundingMode.HALF_EVEN for financial accuracy
    @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    // Optimistic locking version
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(String idempotencyKey, UUID senderAccountId, UUID receiverAccountId, BigDecimal amount, String currency) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if (senderAccountId == null || receiverAccountId == null) {
            throw new IllegalArgumentException("Sender and receiver account IDs are required");
        }
        if (senderAccountId.equals(receiverAccountId)) {
            throw new IllegalArgumentException("Sender and receiver accounts cannot be the same");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be strictly greater than zero");
        }

        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey.trim();
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        this.currency = (currency == null || currency.trim().isEmpty()) ? "INR" : currency.trim().toUpperCase();
        this.status = PaymentStatus.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // State Machine Transitions:
    // INITIATED -> VALIDATED
    public void validate() {
        if (this.status != PaymentStatus.INITIATED) {
            throw new InvalidStateTransitionException(this.status, PaymentStatus.VALIDATED);
        }
        this.status = PaymentStatus.VALIDATED;
        this.updatedAt = Instant.now();
    }

    // VALIDATED -> SETTLED
    public void settle() {
        if (this.status != PaymentStatus.VALIDATED) {
            throw new InvalidStateTransitionException(this.status, PaymentStatus.SETTLED);
        }
        this.status = PaymentStatus.SETTLED;
        this.updatedAt = Instant.now();
    }

    // INITIATED / VALIDATED -> FAILED (Terminal state)
    public void fail(String reason) {
        if (this.status == PaymentStatus.SETTLED || this.status == PaymentStatus.FAILED) {
            throw new InvalidStateTransitionException(this.status, PaymentStatus.FAILED);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public UUID getReceiverAccountId() {
        return receiverAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
