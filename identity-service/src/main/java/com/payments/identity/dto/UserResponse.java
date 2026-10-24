package com.payments.identity.dto;

import com.payments.identity.domain.User;
import com.payments.identity.domain.UserRole;
import com.payments.identity.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

// User profile response DTO
public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        UserRole role,
        UserStatus status,
        int failedLoginAttempts,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getFailedLoginAttempts(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
