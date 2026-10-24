package com.payments.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// @Entity: users table represent karta hai identity_db mein
@Entity
@Table(name = "users")
public class User {

    public static final int MAX_FAILED_ATTEMPTS = 5;

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Invariant: Kabhi bhi plaintext password store nahi hota, sirf BCrypt(12) hash store hoga
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {
        // JPA ke liye default constructor
    }

    private User(UUID id, String username, String email, String fullName, String passwordHash, UserRole role) {
        this.id = id != null ? id : UUID.randomUUID();
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.createdAt = Instant.now();
    }

    public static User create(String username, String email, String fullName, String passwordHash, UserRole role) {
        return new User(UUID.randomUUID(), username, email, fullName, passwordHash, role);
    }

    public static User createWithId(UUID id, String username, String email, String fullName, String passwordHash, UserRole role) {
        return new User(id, username, email, fullName, passwordHash, role);
    }

    // Invariant FR-20: 5 consecutive galat passwords par account automatically lock ho jaata hai
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.status = UserStatus.LOCKED;
        }
    }

    // Login successful hone par failed attempts counter reset ho jaata hai
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = Instant.now();
    }

    // Admin endpoint se account status unlock karne ke liye
    public void unlock() {
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    public boolean isLocked() {
        return this.status == UserStatus.LOCKED;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
