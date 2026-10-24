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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountTransactionsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should record ledger entry on account creation and return transaction history")
    void shouldRecordLedgerAndReturnTransactions() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("Vikram Mehta", new BigDecimal("1500.00"), "INR");

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String accountId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Perform a debit
        TransferAmountRequest debitReq = new TransferAmountRequest(new BigDecimal("300.00"));
        mockMvc.perform(post("/api/accounts/{id}/debit", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isOk());

        // Get transactions
        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].entryType", is("DEBIT")))
                .andExpect(jsonPath("$[0].amount", is(300.0)))
                .andExpect(jsonPath("$[0].balanceAfter", is(1200.0)))
                .andExpect(jsonPath("$[1].entryType", is("CREDIT")))
                .andExpect(jsonPath("$[1].amount", is(1500.0)));

        // Statement CSV export
        mockMvc.perform(get("/api/accounts/{id}/statement/csv", accountId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("TransactionId,AccountId,OwnerName,Timestamp,Type,Amount,BalanceAfter,ReferenceId,Description")))
                .andExpect(content().string(containsString("Vikram Mehta")))
                .andExpect(content().string(containsString("DEBIT,300.00,1200.00")));
    }
}
