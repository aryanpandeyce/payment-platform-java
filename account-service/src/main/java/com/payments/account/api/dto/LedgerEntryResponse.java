package com.payments.account.api.dto;

import com.payments.account.domain.EntryType;
import com.payments.account.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID accountId,
        EntryType entryType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String referenceId,
        String description,
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getAccountId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getReferenceId(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }
}
