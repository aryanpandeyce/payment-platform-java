package com.payments.payment.api.dto;

import com.payments.payment.domain.Payment;
import com.payments.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// DTO (Java record) - Payment details and lifecycle response
@Schema(description = "Payment transfer transaction details")
public record PaymentResponse(
        @Schema(description = "Unique payment transaction UUID", example = "p1000000-0000-0000-0000-000000000001")
        UUID id,

        @Schema(description = "Client-supplied idempotency key", example = "c8942b10-6720-4ff6-9bb5-bb2a440c9f12")
        String idempotencyKey,

        @Schema(description = "Sender account ID", example = "a1000000-0000-0000-0000-000000000001")
        UUID senderAccountId,

        @Schema(description = "Receiver account ID", example = "a2000000-0000-0000-0000-000000000002")
        UUID receiverAccountId,

        @Schema(description = "Transfer amount", example = "500.00")
        BigDecimal amount,

        @Schema(description = "Currency code", example = "INR")
        String currency,

        @Schema(description = "State machine lifecycle status: INITIATED, VALIDATED, SETTLED, FAILED", example = "SETTLED")
        PaymentStatus status,

        @Schema(description = "Reason description if payment reached terminal FAILED status", example = "null")
        String failureReason,

        @Schema(description = "Transaction creation timestamp")
        Instant createdAt,

        @Schema(description = "Last status update timestamp")
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getSenderAccountId(),
                payment.getReceiverAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
