package com.payments.account.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.account.api.dto.AccountResponse;
import com.payments.account.api.dto.CreateAccountRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("FR-21: USER A cannot read USER B's account details, balance or transactions")
    void rbac_UserCannotAccessOtherUserAccount() throws Exception {
        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();

        // 1. Create account owned by User B
        CreateAccountRequest reqB = new CreateAccountRequest("Priya Nair", new BigDecimal("5000.00"), "INR", userBId);
        MvcResult resB = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse accountB = objectMapper.readValue(resB.getResponse().getContentAsString(), AccountResponse.class);
        UUID accountBId = accountB.id();

        // 2. User A attempts to read User B's account -> 403 Forbidden
        mockMvc.perform(get("/api/accounts/{id}", accountBId)
                        .header("X-User-Id", userAId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // 3. User A attempts to read User B's balance -> 403 Forbidden
        mockMvc.perform(get("/api/accounts/{id}/balance", accountBId)
                        .header("X-User-Id", userAId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // 4. User A attempts to view User B's transactions -> 403 Forbidden
        mockMvc.perform(get("/api/accounts/{id}/transactions", accountBId)
                        .header("X-User-Id", userAId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // 5. User B reads own account -> 200 OK
        mockMvc.perform(get("/api/accounts/{id}", accountBId)
                        .header("X-User-Id", userBId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountBId.toString()));

        // 6. ADMIN can read User B's account -> 200 OK
        mockMvc.perform(get("/api/accounts/{id}", accountBId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountBId.toString()));
    }
}
