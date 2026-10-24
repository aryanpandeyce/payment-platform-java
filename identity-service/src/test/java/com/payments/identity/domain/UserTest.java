package com.payments.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("New user should be created with ACTIVE status and 0 failed attempts")
    void createNewUser_ShouldBeActive() {
        User user = User.create(
                "rahul.sharma",
                "rahul.sharma@apexpay.demo",
                "Rahul Sharma",
                "$2a$12$sampleHashedPasswordWithBCryptStrength12...",
                UserRole.USER
        );

        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("rahul.sharma");
        assertThat(user.getEmail()).isEqualTo("rahul.sharma@apexpay.demo");
        assertThat(user.getFullName()).isEqualTo("Rahul Sharma");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("5 consecutive failed logins should automatically transition user to LOCKED")
    void failedLogins_ShouldLockAccountOnFifthAttempt() {
        User user = User.create(
                "test.user",
                "test@apexpay.demo",
                "Test User",
                "$2a$12$sampleHash",
                UserRole.USER
        );

        for (int i = 1; i <= 4; i++) {
            user.recordFailedLogin();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getFailedLoginAttempts()).isEqualTo(i);
        }

        // 5th attempt locks the account
        user.recordFailedLogin();
        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    @DisplayName("Successful login should reset failed attempts counter")
    void successfulLogin_ShouldResetFailedAttempts() {
        User user = User.create(
                "test.user",
                "test@apexpay.demo",
                "Test User",
                "$2a$12$sampleHash",
                UserRole.USER
        );

        user.recordFailedLogin();
        user.recordFailedLogin();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);

        user.recordSuccessfulLogin();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin unlock should restore ACTIVE status and reset failed attempts")
    void unlock_ShouldRestoreActiveStatus() {
        User user = User.create(
                "vikram.singh",
                "vikram.singh@apexpay.demo",
                "Vikram Singh",
                "$2a$12$sampleHash",
                UserRole.USER
        );

        // Lock user
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin();
        }
        assertThat(user.isLocked()).isTrue();

        // Admin unlocks
        user.unlock();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isLocked()).isFalse();
    }
}
