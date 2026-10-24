package com.payments.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// DTO (Java record) - Debit / Credit monetary amount payload
@Schema(description = "Monetary transfer amount payload")
public record TransferAmountRequest(
        @Schema(description = "Amount to debit or credit (must be > 0.00)", example = "500.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount
) {}
