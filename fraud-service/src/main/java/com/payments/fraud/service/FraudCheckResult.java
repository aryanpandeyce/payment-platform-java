package com.payments.fraud.service;

// Fraud evaluation ka result model
public class FraudCheckResult {

    private final boolean passed;
    private final String failureReason;

    private FraudCheckResult(boolean passed, String failureReason) {
        this.passed = passed;
        this.failureReason = failureReason;
    }

    public static FraudCheckResult pass() {
        return new FraudCheckResult(true, null);
    }

    public static FraudCheckResult fail(String reason) {
        return new FraudCheckResult(false, reason);
    }

    public boolean isPassed() {
        return passed;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
