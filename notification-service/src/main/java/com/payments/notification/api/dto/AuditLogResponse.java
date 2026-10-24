package com.payments.notification.api.dto;

import com.payments.notification.domain.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        String id,
        String eventId,
        String eventType,
        String aggregateId,
        Instant timestamp,
        String payload,
        String correlationId
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEventId(),
                log.getEventType(),
                log.getAggregateId(),
                log.getTimestamp(),
                log.getPayload(),
                log.getCorrelationId()
        );
    }
}
