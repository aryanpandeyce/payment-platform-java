package com.payments.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

// DTO (Java record) - Account create karne ke liye incoming request
@Schema(description = "Request payload to create a new customer bank account")
public record CreateAccountRequest(
        @Schema(description = "Account holder legal name", example = "Rahul Sharma")
        @NotBlank(message = "Owner name is required")
        String ownerName,

        @Schema(description = "Opening ledger balance", example = "10000.00")
        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
        BigDecimal initialBalance,

        @Schema(description = "Three-letter ISO currency code", example = "INR", defaultValue = "INR")
        String currency,

        @Schema(description = "User ID who owns this account", example = "00000000-0000-0000-0000-000000000011")
        UUID ownerUserId
) {
    public CreateAccountRequest {
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
    }

    public CreateAccountRequest(String ownerName, BigDecimal initialBalance, String currency) {
        this(ownerName, initialBalance, currency, null);
    }

    public CreateAccountRequest(String ownerName, BigDecimal initialBalance) {
        this(ownerName, initialBalance, "INR", null);
    }
}
