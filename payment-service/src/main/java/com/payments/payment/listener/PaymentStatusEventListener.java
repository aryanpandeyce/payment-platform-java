package com.payments.payment.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.domain.InvalidStateTransitionException;
import com.payments.payment.domain.PaymentNotFoundException;
import com.payments.payment.event.PaymentFailedEvent;
import com.payments.payment.event.PaymentSettledEvent;
import com.payments.payment.event.PaymentValidatedEvent;
import com.payments.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// @Component: Payment status lifecycle coordinator listener
// Kafka se validated, settled aur failed events receive karke state machine updates karta hai
@Component
public class PaymentStatusEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusEventListener.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentStatusEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    // 1. Validated event consumer -> INITIATED to VALIDATED
    @KafkaListener(topics = "payment-validated", groupId = "payment-service-group")
    public void onPaymentValidated(String message) {
        try {
            PaymentValidatedEvent event = objectMapper.readValue(message, PaymentValidatedEvent.class);
            log.info("Received payment-validated event for paymentId: {}", event.getPaymentId());
            paymentService.validatePayment(event.getPaymentId());
            log.info("Payment {} transitioned to VALIDATED", event.getPaymentId());
        } catch (InvalidStateTransitionException | PaymentNotFoundException e) {
            log.info("Idempotent ignore: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed processing payment-validated message: {}", e.getMessage(), e);
        }
    }

    // 2. Settled event consumer -> VALIDATED to SETTLED
    @KafkaListener(topics = "payment-settled", groupId = "payment-service-group")
    public void onPaymentSettled(String message) {
        try {
            PaymentSettledEvent event = objectMapper.readValue(message, PaymentSettledEvent.class);
            log.info("Received payment-settled event for paymentId: {}", event.getPaymentId());
            paymentService.settlePayment(event.getPaymentId());
            log.info("Payment {} transitioned to SETTLED", event.getPaymentId());
        } catch (InvalidStateTransitionException | PaymentNotFoundException e) {
            log.info("Idempotent ignore: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed processing payment-settled message: {}", e.getMessage(), e);
        }
    }

    // 3. Failed event consumer -> INITIATED/VALIDATED to FAILED
    @KafkaListener(topics = "payment-failed", groupId = "payment-service-group")
    public void onPaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            log.info("Received payment-failed event for paymentId: {}, reason: {}",
                    event.getPaymentId(), event.getFailureReason());
            paymentService.failPayment(event.getPaymentId(), event.getFailureReason());
            log.info("Payment {} transitioned to FAILED with reason: {}",
                    event.getPaymentId(), event.getFailureReason());
        } catch (InvalidStateTransitionException | PaymentNotFoundException e) {
            log.info("Idempotent ignore: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed processing payment-failed message: {}", e.getMessage(), e);
        }
    }
}
