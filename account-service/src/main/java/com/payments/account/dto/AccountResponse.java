package com.payments.account.dto;

import com.payments.account.domain.Account;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Account details return karne ke liye HTTP response model
public class AccountResponse {

    private UUID id;
    private String ownerName;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Instant createdAt;

    public AccountResponse() {
    }

    public AccountResponse(UUID id, String ownerName, BigDecimal balance, String currency, String status, Instant createdAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Account entity se DTO convert karne ka helper
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerName(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
