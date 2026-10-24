package com.payments.account.api.dto;

import com.payments.account.domain.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// DTO (Java record) - Account metadata and state
@Schema(description = "Account details and current status")
public record AccountResponse(
        @Schema(description = "Unique account identifier", example = "a1000000-0000-0000-0000-000000000001")
        UUID id,

        @Schema(description = "Owner user ID for RBAC", example = "00000000-0000-0000-0000-000000000001")
        UUID ownerUserId,

        @Schema(description = "Account holder name", example = "Rahul Sharma")
        String ownerName,

        @Schema(description = "Current available balance", example = "9500.00")
        BigDecimal balance,

        @Schema(description = "Account currency", example = "INR")
        String currency,

        @Schema(description = "Lifecycle status: ACTIVE, FROZEN, CLOSED", example = "ACTIVE")
        String status,

        @Schema(description = "Account opening timestamp")
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerUserId(),
                account.getOwnerName(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
