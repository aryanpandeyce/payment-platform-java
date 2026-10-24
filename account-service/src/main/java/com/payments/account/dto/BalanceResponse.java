package com.payments.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

// Sirf balance check karne ke liye lightweight response
public class BalanceResponse {

    private UUID accountId;
    private BigDecimal balance;
    private String currency;
    private String status;

    public BalanceResponse() {
    }

    public BalanceResponse(UUID accountId, BigDecimal balance, String currency, String status) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public UUID getAccountId() {
        return accountId;
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
}
