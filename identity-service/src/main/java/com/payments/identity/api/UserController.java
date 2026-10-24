package com.payments.identity.api;

import com.payments.identity.application.UserService;
import com.payments.identity.domain.UserRole;
import com.payments.identity.dto.UpdateUserStatusRequest;
import com.payments.identity.dto.UserResponse;
import com.payments.identity.exception.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User profile queries and admin status management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns identity details based on X-User-Id header")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return ResponseEntity.ok(userService.getUserById(UUID.fromString(userIdHeader)));
        } else if (usernameHeader != null && !usernameHeader.isBlank()) {
            return ResponseEntity.ok(userService.getUserByUsername(usernameHeader));
        }
        throw new ForbiddenException("Missing authentication user identification header");
    }

    @GetMapping
    @Operation(summary = "List all users (Admin only)", description = "Returns full user list for administrators")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        if (!UserRole.ADMIN.name().equalsIgnoreCase(roleHeader)) {
            throw new ForbiddenException("Access denied: Administrator privileges required");
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status (Admin only)", description = "Lock or unlock a user account")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        if (!UserRole.ADMIN.name().equalsIgnoreCase(roleHeader)) {
            throw new ForbiddenException("Access denied: Administrator privileges required");
        }
        return ResponseEntity.ok(userService.updateUserStatus(id, request.status()));
    }
}
