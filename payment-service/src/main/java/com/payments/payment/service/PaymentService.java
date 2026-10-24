package com.payments.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.domain.*;
import com.payments.payment.dto.InitiatePaymentRequest;
import com.payments.payment.dto.PaymentResponse;
import com.payments.payment.event.PaymentInitiatedEvent;
import com.payments.payment.repository.OutboxRepository;
import com.payments.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// @Service: Payment orchestration and idempotency management
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public record InitiationResult(PaymentResponse response, boolean isNew) {}

    public PaymentService(PaymentRepository paymentRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    // FR-5: Initiate payment with Idempotency Key
    // @Transactional: Payment row aur Outbox row same ACID transaction mein insert honge
    @Transactional
    public InitiationResult initiatePayment(String idempotencyKey, InitiatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (request.getSenderAccountId().equals(request.getReceiverAccountId())) {
            throw new IllegalArgumentException("Sender and receiver account cannot be the same");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be strictly greater than zero");
        }

        Optional<Payment> existingOpt = paymentRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            // Check for payload match
            boolean sameSender = existing.getSenderAccountId().equals(request.getSenderAccountId());
            boolean sameReceiver = existing.getReceiverAccountId().equals(request.getReceiverAccountId());
            boolean sameAmount = existing.getAmount().compareTo(request.getAmount()) == 0;

            if (sameSender && sameReceiver && sameAmount) {
                // Exact same payload: Return existing payment with 200 OK
                return new InitiationResult(PaymentResponse.from(existing), false);
            } else {
                // Same key with different payload: 409 Conflict
                throw new IdempotencyPayloadMismatchException(idempotencyKey.trim());
            }
        }

        // New Payment aggregate creation
        Payment payment = new Payment(
                idempotencyKey.trim(),
                request.getSenderAccountId(),
                request.getReceiverAccountId(),
                request.getAmount(),
                request.getCurrency()
        );
        Payment savedPayment = paymentRepository.save(payment);

        // Outbox Pattern: Save event in same DB transaction
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                savedPayment.getId(),
                savedPayment.getSenderAccountId(),
                savedPayment.getReceiverAccountId(),
                savedPayment.getAmount(),
                savedPayment.getCurrency()
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    "PAYMENT",
                    savedPayment.getId(),
                    "PaymentInitiated",
                    payloadJson
            );
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        return new InitiationResult(PaymentResponse.from(savedPayment), true);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    // State machine status updates (called by event consumers in Phase 3)
    @Transactional
    public PaymentResponse validatePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.validate();
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse settlePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.settle();
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse failPayment(UUID id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.fail(reason);
        return PaymentResponse.from(paymentRepository.save(payment));
    }
}
