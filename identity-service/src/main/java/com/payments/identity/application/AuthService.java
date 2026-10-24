package com.payments.identity.application;

import com.payments.identity.domain.*;
import com.payments.identity.dto.*;
import com.payments.identity.exception.AccountLockedException;
import com.payments.identity.exception.InvalidCredentialsException;
import com.payments.identity.exception.InvalidRefreshTokenException;
import com.payments.identity.exception.UserAlreadyExistsException;
import com.payments.identity.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordPolicy passwordPolicy;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long refreshTokenExpirationSeconds;
    private final io.micrometer.core.instrument.Counter loginFailuresCounter;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordPolicy passwordPolicy,
            JwtTokenProvider jwtTokenProvider,
            io.micrometer.core.instrument.MeterRegistry meterRegistry,
            @Value("${jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordPolicy = passwordPolicy;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        // Invariant: BCrypt cost factor 12 for high hashing security
        this.passwordEncoder = new BCryptPasswordEncoder(12);
        io.micrometer.core.instrument.MeterRegistry registry =
                meterRegistry != null ? meterRegistry : new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        this.loginFailuresCounter = io.micrometer.core.instrument.Counter.builder("auth_login_failures_total")
                .description("Total number of failed user login attempts")
                .register(registry);
    }

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordPolicy passwordPolicy,
            JwtTokenProvider jwtTokenProvider,
            long refreshTokenExpirationSeconds) {
        this(userRepository, refreshTokenRepository, passwordPolicy, jwtTokenProvider, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), refreshTokenExpirationSeconds);
    }

    // FR-17: New user registration with password policy check
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!passwordPolicy.isValid(request.password())) {
            throw new IllegalArgumentException("Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, and one digit");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email '" + request.email() + "' is already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = User.create(
                request.username(),
                request.email(),
                request.fullName(),
                hashedPassword,
                UserRole.USER
        );

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    // FR-18 & FR-20: Authentication, lockout on 5 failed attempts, dual-token issuance
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    loginFailuresCounter.increment();
                    return new InvalidCredentialsException("Invalid username or password");
                });

        if (user.isLocked()) {
            loginFailuresCounter.increment();
            throw new AccountLockedException("Account is locked due to repeated failed login attempts. Please contact an administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            loginFailuresCounter.increment();
            if (user.isLocked()) {
                throw new AccountLockedException("Account is locked due to repeated failed login attempts. Please contact an administrator.");
            }
            throw new InvalidCredentialsException("Invalid username or password");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rawRefreshToken = generateSecureToken();
        String tokenHash = hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plus(refreshTokenExpirationSeconds, ChronoUnit.SECONDS);
        RefreshToken refreshToken = RefreshToken.create(user.getId(), tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.of(
                accessToken,
                jwtTokenProvider.getExpirationSeconds(),
                rawRefreshToken,
                UserResponse.from(user)
        );
    }

    // FR-19: Refresh token rotation - single use, invalidates previous token
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh token"));

        if (!token.isValid()) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User account not found"));

        if (user.isLocked()) {
            throw new AccountLockedException("User account is locked");
        }

        // Revoke the old refresh token (Token Rotation invariant)
        token.revoke();
        refreshTokenRepository.save(token);

        // Issue new token pair
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRawRefreshToken = generateSecureToken();
        String newTokenHash = hashToken(newRawRefreshToken);

        Instant expiresAt = Instant.now().plus(refreshTokenExpirationSeconds, ChronoUnit.SECONDS);
        RefreshToken newRefreshToken = RefreshToken.create(user.getId(), newTokenHash, expiresAt);
        refreshTokenRepository.save(newRefreshToken);

        return RefreshTokenResponse.of(
                newAccessToken,
                jwtTokenProvider.getExpirationSeconds(),
                newRawRefreshToken
        );
    }

    // FR-19: Logout revokes session refresh token
    @Transactional
    public void logout(LogoutRequest request) {
        if (request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenHash = hashToken(request.refreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
