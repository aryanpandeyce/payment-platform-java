package com.payments.notification.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.notification.domain.AuditLog;
import com.payments.notification.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// @Component: FR-13 Centralized immutable audit trail consumer for MongoDB
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditEventListener(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    // Har ek lifecycle domain event ko MongoDB audit_logs collection mein append karta hai
    @KafkaListener(topics = {"payment-initiated", "payment-validated", "payment-settled", "payment-failed"},
                   groupId = "audit-service-group")
    public void onDomainEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String paymentId = root.has("paymentId") ? root.get("paymentId").asText() : "UNKNOWN";
            String eventId = root.has("eventId") ? root.get("eventId").asText() : UUID.randomUUID().toString();

            String eventType;
            if (root.has("failureReason")) {
                eventType = "PaymentFailed";
            } else if (root.has("status") && root.get("status").asText().equals("INITIATED")) {
                eventType = "PaymentInitiated";
            } else if (root.has("senderAccountId") && root.has("receiverAccountId")) {
                // If it's validated or settled
                eventType = root.has("currency") ? "PaymentValidatedOrSettled" : "PaymentDomainEvent";
            } else {
                eventType = "PaymentEvent";
            }

            AuditLog logEntry = new AuditLog(
                    eventId,
                    eventType,
                    paymentId,
                    Instant.now(),
                    message,
                    UUID.randomUUID().toString()
            );

            auditLogRepository.save(logEntry);
            log.info("Appended immutable audit log in MongoDB for aggregate: {}, type: {}", paymentId, eventType);

        } catch (Exception e) {
            log.error("Failed to append audit log to MongoDB: {}", e.getMessage(), e);
        }
    }
}
