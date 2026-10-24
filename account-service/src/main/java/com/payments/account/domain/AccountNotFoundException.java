package com.payments.account.domain;

import java.util.UUID;

// Account na milne par throw hone wala exception
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID id) {
        super("Account not found with ID: " + id);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
