package com.payments.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    @DisplayName("Valid password meeting all policy criteria should pass")
    void validPassword_ShouldPass() {
        // Upper, lower, digit, >= 8 chars
        assertThat(passwordPolicy.isValid("SecurePass123")).isTrue();
        assertThat(passwordPolicy.isValid("AryanAdmin#2026")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1A",          // less than 8 characters
            "nouppercase123",   // no uppercase letter
            "NOLOWERCASE123",   // no lowercase letter
            "NoDigitsAllowed",  // no digit
            "",                 // empty
            "        "          // whitespace only
    })
    @DisplayName("Passwords violating policy should be rejected")
    void invalidPassword_ShouldFail(String invalidPassword) {
        assertThat(passwordPolicy.isValid(invalidPassword)).isFalse();
    }
}
