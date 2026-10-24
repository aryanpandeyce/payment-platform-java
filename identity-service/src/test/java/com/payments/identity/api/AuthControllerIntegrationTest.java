package com.payments.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.identity.domain.UserRepository;
import com.payments.identity.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Seed data is loaded by default
    }

    @Test
    @DisplayName("POST /auth/register with valid data should create user and return 201 Created")
    void register_ValidData_ShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "new.customer",
                "new.customer@apexpay.demo",
                "New Customer",
                "ValidPass123"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new.customer"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /auth/register with weak password should return 400 Bad Request")
    void register_WeakPassword_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "weak.user",
                "weak@apexpay.demo",
                "Weak User",
                "weakpass" // no uppercase, no digit
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /auth/register with duplicate username should return 409 Conflict")
    void register_DuplicateUsername_ShouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "aryan.pandey",
                "another.aryan@apexpay.demo",
                "Another Aryan",
                "Password123"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /auth/login with valid credentials should return tokens and user details")
    void login_ValidCredentials_ShouldReturnTokens() throws Exception {
        LoginRequest request = new LoginRequest("aryan.pandey", "AryanAdmin#2026");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("aryan.pandey"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /auth/login with invalid password should return 401 Unauthorized")
    void login_InvalidPassword_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("aryan.pandey", "WrongPassword123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /auth/login on locked account (Vikram Singh) should return 403 Forbidden")
    void login_LockedAccount_ShouldReturn403() throws Exception {
        LoginRequest request = new LoginRequest("vikram.singh", "DemoUser#2026");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("POST /auth/refresh should rotate refresh token and issue new token pair")
    void refresh_ValidToken_ShouldRotate() throws Exception {
        // Step 1: Login to get a refresh token
        LoginRequest loginRequest = new LoginRequest("rahul.sharma", "DemoUser#2026");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        String initialRefreshToken = loginResponse.refreshToken();

        // Step 2: Call /auth/refresh with initial token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        RefreshTokenResponse refreshResponse = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), RefreshTokenResponse.class);
        String newRefreshToken = refreshResponse.refreshToken();
        assertThat(newRefreshToken).isNotEqualTo(initialRefreshToken);

        // Step 3: Reusing old token should fail (Token Rotation invariant)
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("POST /auth/logout should revoke refresh token")
    void logout_ShouldRevokeToken() throws Exception {
        // Login
        LoginRequest loginRequest = new LoginRequest("priya.nair", "DemoUser#2026");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Logout
        LogoutRequest logoutRequest = new LogoutRequest(loginResponse.refreshToken());
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Refresh with logged out token should fail
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(loginResponse.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }
}
