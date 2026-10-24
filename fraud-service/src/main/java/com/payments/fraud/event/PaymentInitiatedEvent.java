package com.payments.fraud.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentInitiatedEvent {

    private UUID eventId;
    private UUID paymentId;
    private UUID senderAccountId;
    private UUID receiverAccountId;
    private BigDecimal amount;
    private String currency;
    private Instant timestamp;

    public PaymentInitiatedEvent() {
    }

    public PaymentInitiatedEvent(UUID paymentId, UUID senderAccountId, UUID receiverAccountId, BigDecimal amount, String currency) {
        this.eventId = UUID.randomUUID();
        this.paymentId = paymentId;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getPaymentId() {
        return paymentId;
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

    public Instant getTimestamp() {
        return timestamp;
    }
}
