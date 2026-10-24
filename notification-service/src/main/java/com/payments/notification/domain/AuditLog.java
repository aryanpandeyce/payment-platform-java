package com.payments.notification.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// @Document: MongoDB collection "audit_logs" mein append-only document persist karega
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;
    private String eventId;
    private String eventType;
    private String aggregateId;
    private Instant timestamp;
    private String payload;
    private String correlationId;

    public AuditLog() {
    }

    public AuditLog(String eventId, String eventType, String aggregateId, Instant timestamp, String payload, String correlationId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.payload = payload;
        this.correlationId = correlationId;
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPayload() {
        return payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
