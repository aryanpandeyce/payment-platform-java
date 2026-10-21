package com.payments.account.domain;

// Jab account me balance kam ho aur user debit karna chahe, tab ye custom exception throw hoga.
// RuntimeException extend kar rahe hain taaki ye unchecked exception bane (Spring @Transactional me automatically rollback hota hai).
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
