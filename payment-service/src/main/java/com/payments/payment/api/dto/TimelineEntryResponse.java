package com.payments.payment.api.dto;

import com.payments.payment.domain.PaymentStatus;
import com.payments.payment.domain.PaymentTimeline;

import java.time.Instant;
import java.util.UUID;

public record TimelineEntryResponse(
        UUID id,
        UUID paymentId,
        PaymentStatus status,
        Instant timestamp,
        String failureReason
) {
    public static TimelineEntryResponse from(PaymentTimeline timeline) {
        return new TimelineEntryResponse(
                timeline.getId(),
                timeline.getPaymentId(),
                timeline.getStatus(),
                timeline.getTimestamp(),
                timeline.getFailureReason()
        );
    }
}
