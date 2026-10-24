package com.payments.payment.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInitiatedEvent(
        UUID paymentId,
        String idempotencyKey,
        UUID senderAccountId,
        UUID receiverAccountId,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {}
