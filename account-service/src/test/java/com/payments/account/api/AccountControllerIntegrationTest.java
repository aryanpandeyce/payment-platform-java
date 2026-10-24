package com.payments.account.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.account.api.dto.CreateAccountRequest;
import com.payments.account.api.dto.TransferAmountRequest;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("FR-1: POST /api/accounts creates new account with 201 Created")
    void shouldCreateAccount() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest("Rahul Sharma", new BigDecimal("1000.00"), "INR");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.ownerName", is("Rahul Sharma")))
                .andExpect(jsonPath("$.balance", is(1000.00)))
                .andExpect(jsonPath("$.currency", is("INR")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("FR-1: POST /api/accounts with negative initial balance returns 400 Bad Request")
    void shouldRejectNegativeInitialBalance() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest("Rahul Sharma", new BigDecimal("-50.00"), "INR");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_INITIAL_BALANCE")));
    }

    @Test
    @DisplayName("FR-2: GET /api/accounts/{id} returns 200 OK and account details")
    void shouldGetAccountById() throws Exception {
        // Step 1: Create account
        CreateAccountRequest req = new CreateAccountRequest("Priya Patel", new BigDecimal("500.00"));
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String accountId = objectMapper.readTree(responseJson).get("id").asText();

        // Step 2: Fetch account
        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(accountId)))
                .andExpect(jsonPath("$.ownerName", is("Priya Patel")))
                .andExpect(jsonPath("$.balance", is(500.00)));

        // Step 3: Fetch balance endpoint
        mockMvc.perform(get("/api/accounts/" + accountId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(accountId)))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    @DisplayName("FR-2: GET /api/accounts/{id} with non-existent ID returns 404 Not Found")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/accounts/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ACCOUNT_NOT_FOUND")));
    }

    @Test
    @DisplayName("FR-3: POST /api/accounts/{id}/debit reduces balance or returns 409 when insufficient funds")
    void shouldDebitAndHandleInsufficientFunds() throws Exception {
        // Create account with 300.00
        CreateAccountRequest createReq = new CreateAccountRequest("Karan Singh", new BigDecimal("300.00"));
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Debit 100.00 -> should succeed, balance 200.00
        TransferAmountRequest debitReq = new TransferAmountRequest(new BigDecimal("100.00"));
        mockMvc.perform(post("/api/accounts/" + accountId + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(200.00)));

        // Debit 500.00 -> balance is only 200.00, should return 409 Conflict
        TransferAmountRequest overDebitReq = new TransferAmountRequest(new BigDecimal("500.00"));
        mockMvc.perform(post("/api/accounts/" + accountId + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overDebitReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_FUNDS")));
    }

    @Test
    @DisplayName("FR-4: POST /api/accounts/{id}/credit increases balance atomically")
    void shouldCreditAccount() throws Exception {
        // Create account with 100.00
        CreateAccountRequest createReq = new CreateAccountRequest("Anjali Sharma", new BigDecimal("100.00"));
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Credit 250.50 -> balance should be 350.50
        TransferAmountRequest creditReq = new TransferAmountRequest(new BigDecimal("250.50"));
        mockMvc.perform(post("/api/accounts/" + accountId + "/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(350.50)));
    }
}
