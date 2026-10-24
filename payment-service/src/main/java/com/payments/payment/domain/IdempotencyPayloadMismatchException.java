package com.payments.payment.domain;

// Same Idempotency-Key ke sath doosra payload bhejne par throw hota hai (HTTP 409 Conflict)
public class IdempotencyPayloadMismatchException extends RuntimeException {
    public IdempotencyPayloadMismatchException(String key) {
        super("Idempotency key '" + key + "' was previously used with a different request payload.");
    }
}
