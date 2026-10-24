package com.payments.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

// DTO (Java record) - Payment initiation request payload
@Schema(description = "Request payload to initiate a new money transfer between accounts")
public record InitiatePaymentRequest(
        @Schema(description = "Sender account UUID", example = "a1000000-0000-0000-0000-000000000001")
        @NotNull(message = "Sender account ID is required")
        UUID senderAccountId,

        @Schema(description = "Receiver account UUID", example = "a2000000-0000-0000-0000-000000000002")
        @NotNull(message = "Receiver account ID is required")
        UUID receiverAccountId,

        @Schema(description = "Transfer monetary amount (strictly > 0.00)", example = "500.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount,

        @Schema(description = "ISO currency code", example = "INR", defaultValue = "INR")
        String currency
) {
    public InitiatePaymentRequest {
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
    }

    public InitiatePaymentRequest(UUID senderAccountId, UUID receiverAccountId, BigDecimal amount) {
        this(senderAccountId, receiverAccountId, amount, "INR");
    }
}
