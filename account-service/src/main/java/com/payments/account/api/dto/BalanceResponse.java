package com.payments.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

// DTO (Java record) - Lightweight balance check response
@Schema(description = "Account balance information")
public record BalanceResponse(
        @Schema(description = "Account UUID", example = "a1000000-0000-0000-0000-000000000001")
        UUID accountId,

        @Schema(description = "Current available balance", example = "9500.00")
        BigDecimal balance,

        @Schema(description = "Currency code", example = "INR")
        String currency,

        @Schema(description = "Account status", example = "ACTIVE")
        String status
) {}
