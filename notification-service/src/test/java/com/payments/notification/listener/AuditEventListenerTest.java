package com.payments.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.notification.domain.AuditLog;
import com.payments.notification.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class AuditEventListenerTest {

    @Test
    @DisplayName("FR-13: Appends domain event into MongoDB audit collection")
    void shouldPersistAuditLog() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AuditEventListener listener = new AuditEventListener(repository, objectMapper);

        UUID paymentId = UUID.randomUUID();
        String json = "{\"paymentId\":\"" + paymentId + "\",\"status\":\"INITIATED\",\"amount\":500.00}";

        listener.onDomainEvent(json);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(paymentId.toString(), saved.getAggregateId());
        assertEquals("PaymentInitiated", saved.getEventType());
        assertNotNull(saved.getTimestamp());
        assertNotNull(saved.getPayload());
    }
}
