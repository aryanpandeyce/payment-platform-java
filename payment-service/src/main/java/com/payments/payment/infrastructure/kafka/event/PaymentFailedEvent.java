package com.payments.payment.infrastructure.kafka.event;

import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        String failureReason
) {}
