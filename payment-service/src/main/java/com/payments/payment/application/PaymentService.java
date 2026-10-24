package com.payments.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.api.dto.PaymentResponse;
import com.payments.payment.api.dto.TimelineEntryResponse;
import com.payments.payment.domain.*;
import com.payments.payment.infrastructure.kafka.event.PaymentInitiatedEvent;
import com.payments.payment.infrastructure.persistence.OutboxRepository;
import com.payments.payment.infrastructure.persistence.PaymentRepository;
import com.payments.payment.infrastructure.persistence.PaymentTimelineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Application Service: Coordinates idempotent payments, status lifecycle, timeline, and transactional outbox persistence
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final PaymentTimelineRepository paymentTimelineRepository;
    private final ObjectMapper objectMapper;

    public record InitiationResult(PaymentResponse response, boolean isNew) {}

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxRepository outboxRepository,
                          PaymentTimelineRepository paymentTimelineRepository,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.paymentTimelineRepository = paymentTimelineRepository;
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
                return new InitiationResult(PaymentResponse.from(existing, getTimeline(existing.getId())), false);
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

        // Record initial timeline entry
        paymentTimelineRepository.save(new PaymentTimeline(
                savedPayment.getId(),
                PaymentStatus.INITIATED,
                savedPayment.getCreatedAt(),
                null
        ));

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

        return new InitiationResult(PaymentResponse.from(savedPayment, getTimeline(savedPayment.getId())), true);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return PaymentResponse.from(payment, getTimeline(payment.getId()));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> PaymentResponse.from(p, getTimeline(p.getId())))
                .collect(Collectors.toList());
    }

    // Phase B: Timeline entries for a payment
    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> getTimeline(UUID paymentId) {
        return paymentTimelineRepository.findByPaymentIdOrderByTimestampAsc(paymentId)
                .stream()
                .map(TimelineEntryResponse::from)
                .toList();
    }

    // Phase B: Search, Filter, Pagination
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(PaymentStatus status, Instant from, Instant to, String search, Pageable pageable) {
        Specification<Payment> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim();
            spec = spec.and((root, query, cb) -> {
                try {
                    UUID searchUuid = UUID.fromString(term);
                    return cb.or(
                            cb.equal(root.get("id"), searchUuid),
                            cb.like(cb.lower(root.get("idempotencyKey")), "%" + term.toLowerCase() + "%")
                    );
                } catch (IllegalArgumentException e) {
                    return cb.like(cb.lower(root.get("idempotencyKey")), "%" + term.toLowerCase() + "%");
                }
            });
        }

        return paymentRepository.findAll(spec, pageable)
                .map(p -> PaymentResponse.from(p, getTimeline(p.getId())));
    }

    // State machine status updates + timeline tracking
    @Transactional
    public PaymentResponse validatePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.validate();
        Payment updated = paymentRepository.save(payment);
        paymentTimelineRepository.save(new PaymentTimeline(id, PaymentStatus.VALIDATED, Instant.now(), null));
        return PaymentResponse.from(updated, getTimeline(id));
    }

    @Transactional
    public PaymentResponse settlePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.settle();
        Payment updated = paymentRepository.save(payment);
        paymentTimelineRepository.save(new PaymentTimeline(id, PaymentStatus.SETTLED, Instant.now(), null));
        return PaymentResponse.from(updated, getTimeline(id));
    }

    @Transactional
    public PaymentResponse failPayment(UUID id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.fail(reason);
        Payment updated = paymentRepository.save(payment);
        paymentTimelineRepository.save(new PaymentTimeline(id, PaymentStatus.FAILED, Instant.now(), reason));
        return PaymentResponse.from(updated, getTimeline(id));
    }
}
