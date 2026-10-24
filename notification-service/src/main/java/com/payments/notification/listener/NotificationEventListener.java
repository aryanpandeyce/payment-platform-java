package com.payments.notification.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// @Component: FR-12 Mock SMS/Email console notification generator
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final ObjectMapper objectMapper;

    public NotificationEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-settled", groupId = "notification-sms-group")
    public void onPaymentSettled(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String paymentId = root.path("paymentId").asText();
            String amount = root.path("amount").asText();
            String senderId = root.path("senderAccountId").asText();
            String receiverId = root.path("receiverAccountId").asText();

            log.info("[NOTIFICATION] [SMS/EMAIL] Transfer of {} from Account {} to Account {} completed successfully. Payment ID: {}",
                    amount, senderId, receiverId, paymentId);
        } catch (Exception e) {
            log.error("Failed to process notification for payment-settled: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "notification-sms-group")
    public void onPaymentFailed(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String paymentId = root.path("paymentId").asText();
            String reason = root.path("failureReason").asText();

            log.info("[NOTIFICATION] [SMS/EMAIL] Transfer failed. Reason: {}. Payment ID: {}",
                    reason, paymentId);
        } catch (Exception e) {
            log.error("Failed to process notification for payment-failed: {}", e.getMessage(), e);
        }
    }
}
