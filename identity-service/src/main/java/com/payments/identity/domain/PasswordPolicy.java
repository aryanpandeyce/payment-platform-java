package com.payments.identity.domain;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

// Password policy enforcement: Minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 digit
@Component
public class PasswordPolicy {

    // Regex pattern for strong password requirements
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");

    public boolean isValid(String password) {
        if (password == null || password.trim().length() < 8) {
            return false;
        }
        return HAS_UPPERCASE.matcher(password).find()
                && HAS_LOWERCASE.matcher(password).find()
                && HAS_DIGIT.matcher(password).find();
    }
}
