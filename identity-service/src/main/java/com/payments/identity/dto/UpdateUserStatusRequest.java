package com.payments.identity.dto;

import com.payments.identity.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Status cannot be null")
        UserStatus status
) {}
