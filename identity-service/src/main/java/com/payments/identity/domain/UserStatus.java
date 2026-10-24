package com.payments.identity.domain;

// Account status: ACTIVE can authenticate, LOCKED is blocked due to failed attempts
public enum UserStatus {
    ACTIVE,
    LOCKED
}
