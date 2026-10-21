package com.payments.account.domain;

// Jab amount <= 0 ho (negative ya zero) ya null ho, tab ye exception throw hoga.
// Banking rule: Har transaction strictly > 0 honi chahiye.
public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
