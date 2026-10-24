package com.payments.common.util;

import java.util.regex.Pattern;

/**
 * DataMaskingUtil — Sensitive banking data protection utility.
 * Sensitive fields (passwords, JWT tokens, account IDs) ko logs ya output
 * mein leak hone se rokne ke liye mask karta hai.
 */
public final class DataMaskingUtil {

    private static final Pattern JSON_PASSWORD_PATTERN =
            Pattern.compile("\"password\"\\s*:\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);

    private static final Pattern KEY_VALUE_SENSITIVE_PATTERN =
            Pattern.compile("(password|token|secret)=([^\\s,]+)", Pattern.CASE_INSENSITIVE);

    private DataMaskingUtil() {
        // Utility class, no instantiation
    }

    /**
     * Account UUID ya number ko mask karta hai — sirf last 4 characters visible rehte hain.
     */
    public static String maskAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "****";
        }
        String clean = accountId.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        return "****-" + clean.substring(clean.length() - 4);
    }

    /**
     * JSON payload mein se password values ko "[PROTECTED]" se replace karta hai.
     */
    public static String maskJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        return JSON_PASSWORD_PATTERN.matcher(json).replaceAll("\"password\":\"[PROTECTED]\"");
    }

    /**
     * Authorization Bearer token ke signature ko mask karta hai.
     */
    public static String maskToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return "[PROTECTED]";
        }
        if (authHeader.startsWith("Bearer ") && authHeader.length() > 25) {
            return authHeader.substring(0, 23) + "...[PROTECTED]";
        }
        return "[PROTECTED]";
    }

    /**
     * General log string mein se key=value credentials ko sanitize karta hai.
     */
    public static String maskLogMessage(String rawLog) {
        if (rawLog == null || rawLog.isBlank()) {
            return rawLog;
        }
        return KEY_VALUE_SENSITIVE_PATTERN.matcher(rawLog).replaceAll("$1=******");
    }
}
