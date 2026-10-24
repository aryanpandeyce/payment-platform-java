package com.payments.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentValidatedEvent(
    String paymentId,
    String senderAccountId,
    String receiverAccountId,
    BigDecimal amount,
    String currency,
    Instant timestamp,
    String correlationId
) {}
