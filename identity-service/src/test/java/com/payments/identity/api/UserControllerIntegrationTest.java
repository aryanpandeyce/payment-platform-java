package com.payments.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.identity.domain.UserStatus;
import com.payments.identity.dto.LoginRequest;
import com.payments.identity.dto.UpdateUserStatusRequest;
import com.payments.identity.infrastructure.seed.IdentityDemoDataSeeder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/users/me with X-User-Id header should return current user profile")
    void getMe_WithHeader_ShouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", IdentityDemoDataSeeder.ADMIN_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(IdentityDemoDataSeeder.ADMIN_ID.toString()))
                .andExpect(jsonPath("$.username").value("aryan.pandey"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/users with X-User-Role: ADMIN should return full user list")
    void getAllUsers_AsAdmin_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(9)));
    }

    @Test
    @DisplayName("GET /api/users with X-User-Role: USER should return 403 Forbidden")
    void getAllUsers_AsUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/status as ADMIN should unlock locked user and allow login")
    void unlockUser_AsAdmin_ShouldAllowLogin() throws Exception {
        // Step 1: Verify Vikram Singh is locked
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("vikram.singh", "DemoUser#2026"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));

        // Step 2: Admin unlocks Vikram Singh
        UpdateUserStatusRequest unlockRequest = new UpdateUserStatusRequest(UserStatus.ACTIVE);
        mockMvc.perform(patch("/api/users/{id}/status", IdentityDemoDataSeeder.VIKRAM_ID)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unlockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.failedLoginAttempts").value(0));

        // Step 3: Vikram Singh can now successfully log in!
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("vikram.singh", "DemoUser#2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("vikram.singh"));
    }
}
