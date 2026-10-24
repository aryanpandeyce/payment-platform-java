package com.payments.common.event;

import java.time.Instant;

public record PaymentFailedEvent(
    String paymentId,
    String failureReason,
    Instant timestamp,
    String correlationId
) {}
