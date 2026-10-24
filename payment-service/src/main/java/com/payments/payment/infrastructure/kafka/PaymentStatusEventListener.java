package com.payments.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.application.PaymentService;
import com.payments.payment.domain.InvalidStateTransitionException;
import com.payments.payment.domain.PaymentNotFoundException;
import com.payments.payment.infrastructure.kafka.event.PaymentFailedEvent;
import com.payments.payment.infrastructure.kafka.event.PaymentSettledEvent;
import com.payments.payment.infrastructure.kafka.event.PaymentValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// Payment status lifecycle coordinator listener: consumes Kafka events and updates status state machine
@Component
public class PaymentStatusEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusEventListener.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentStatusEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    private void withCorrelation(byte[] correlationIdBytes, Runnable action) {
        String correlationId = (correlationIdBytes != null && correlationIdBytes.length > 0)
                ? new String(correlationIdBytes, StandardCharsets.UTF_8)
                : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            action.run();
        } finally {
            MDC.remove("correlationId");
        }
    }

    // 1. Validated event consumer -> INITIATED to VALIDATED
    @KafkaListener(topics = "payment-validated", groupId = "payment-service-group")
    public void onPaymentValidated(@Payload String message,
                                   @Header(name = CORRELATION_ID_HEADER, required = false) byte[] correlationId) {
        withCorrelation(correlationId, () -> {
            try {
                PaymentValidatedEvent event = objectMapper.readValue(message, PaymentValidatedEvent.class);
                log.info("Received payment-validated event for paymentId: {}", event.paymentId());
                paymentService.validatePayment(event.paymentId());
                log.info("Payment {} transitioned to VALIDATED", event.paymentId());
            } catch (InvalidStateTransitionException | PaymentNotFoundException e) {
                log.info("Idempotent ignore: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed processing payment-validated message: {}", e.getMessage(), e);
            }
        });
    }

    // 2. Settled event consumer -> VALIDATED to SETTLED
    @KafkaListener(topics = "payment-settled", groupId = "payment-service-group")
    public void onPaymentSettled(@Payload String message,
                                 @Header(name = CORRELATION_ID_HEADER, required = false) byte[] correlationId) {
        withCorrelation(correlationId, () -> {
            try {
                PaymentSettledEvent event = objectMapper.readValue(message, PaymentSettledEvent.class);
                log.info("Received payment-settled event for paymentId: {}", event.paymentId());
                paymentService.settlePayment(event.paymentId());
                log.info("Payment {} transitioned to SETTLED", event.paymentId());
            } catch (InvalidStateTransitionException | PaymentNotFoundException e) {
                log.info("Idempotent ignore: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed processing payment-settled message: {}", e.getMessage(), e);
            }
        });
    }

    // 3. Failed event consumer -> INITIATED/VALIDATED to FAILED
    @KafkaListener(topics = "payment-failed", groupId = "payment-service-group")
    public void onPaymentFailed(@Payload String message,
                                @Header(name = CORRELATION_ID_HEADER, required = false) byte[] correlationId) {
        withCorrelation(correlationId, () -> {
            try {
                PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
                log.info("Received payment-failed event for paymentId: {}, reason: {}", event.paymentId(), event.failureReason());
                paymentService.failPayment(event.paymentId(), event.failureReason());
                log.info("Payment {} transitioned to FAILED (reason: {})", event.paymentId(), event.failureReason());
            } catch (InvalidStateTransitionException | PaymentNotFoundException e) {
                log.info("Idempotent ignore: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed processing payment-failed message: {}", e.getMessage(), e);
            }
        });
    }
}
