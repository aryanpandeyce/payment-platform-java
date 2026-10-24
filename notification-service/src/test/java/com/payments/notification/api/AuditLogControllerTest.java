package com.payments.notification.api;

import com.payments.notification.api.dto.AuditLogResponse;
import com.payments.notification.application.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("Should query audit logs with filters and pagination")
    void shouldReturnFilteredAuditLogs() throws Exception {
        AuditLogResponse log1 = new AuditLogResponse(
                "log-1",
                "evt-1",
                "PaymentSettled",
                "pay-123",
                Instant.now(),
                "{\"amount\":500.00}",
                "corr-abc"
        );

        when(auditLogService.searchAuditLogs(eq("PaymentSettled"), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log1)));

        mockMvc.perform(get("/api/audit-logs")
                        .param("eventType", "PaymentSettled")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventType", is("PaymentSettled")))
                .andExpect(jsonPath("$.content[0].aggregateId", is("pay-123")))
                .andExpect(jsonPath("$.content[0].correlationId", is("corr-abc")));
    }
}
