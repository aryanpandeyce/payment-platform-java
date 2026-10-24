package com.payments.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// Account create karne ke liye incoming HTTP request body
public class CreateAccountRequest {

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    private BigDecimal initialBalance;

    private String currency = "INR";

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String ownerName, BigDecimal initialBalance) {
        this.ownerName = ownerName;
        this.initialBalance = initialBalance;
        this.currency = "INR";
    }

    public CreateAccountRequest(String ownerName, BigDecimal initialBalance, String currency) {
        this.ownerName = ownerName;
        this.initialBalance = initialBalance;
        this.currency = currency;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
