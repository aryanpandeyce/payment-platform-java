package com.payments.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.api.dto.PaymentResponse;
import com.payments.payment.domain.*;
import com.payments.payment.infrastructure.kafka.event.PaymentInitiatedEvent;
import com.payments.payment.infrastructure.persistence.OutboxRepository;
import com.payments.payment.infrastructure.persistence.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Application Service: Coordinates idempotent payments, status lifecycle, and transactional outbox persistence
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
    @Transactional
    public InitiationResult initiatePayment(String idempotencyKey, InitiatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (request.senderAccountId().equals(request.receiverAccountId())) {
            throw new IllegalArgumentException("Sender and receiver account cannot be the same");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be strictly greater than zero");
        }

        Optional<Payment> existingOpt = paymentRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            // Check for payload match
            boolean sameSender = existing.getSenderAccountId().equals(request.senderAccountId());
            boolean sameReceiver = existing.getReceiverAccountId().equals(request.receiverAccountId());
            boolean sameAmount = existing.getAmount().compareTo(request.amount()) == 0;

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
                request.senderAccountId(),
                request.receiverAccountId(),
                request.amount(),
                request.currency()
        );
        Payment savedPayment = paymentRepository.save(payment);

        // Outbox Pattern: Save event in same DB transaction
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                savedPayment.getId(),
                savedPayment.getIdempotencyKey(),
                savedPayment.getSenderAccountId(),
                savedPayment.getReceiverAccountId(),
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                Instant.now()
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

    // State machine status updates
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
