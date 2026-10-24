package com.payments.account.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentValidatedEvent(
        UUID paymentId,
        UUID senderAccountId,
        UUID receiverAccountId,
        BigDecimal amount,
        String currency
) {}
