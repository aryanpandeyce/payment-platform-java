package com.payments.payment.controller;

import com.payments.payment.dto.InitiatePaymentRequest;
import com.payments.payment.dto.PaymentResponse;
import com.payments.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// @RestController: Payment API endpoints handler
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // FR-5: Initiate payment with Idempotency Key header
    // Same key + same body -> 200 OK (existing payment)
    // Same key + diff body -> 409 Conflict
    // New key -> 202 Accepted (initiated)
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {

        PaymentService.InitiationResult result = paymentService.initiatePayment(idempotencyKey, request);

        if (result.isNew()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.response());
        } else {
            return ResponseEntity.ok(result.response());
        }
    }

    // FR-6: Get payment by ID
    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable("id") UUID id) {
        return paymentService.getPayment(id);
    }

    // Get all payments (for frontend dashboard transactions list)
    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }
}
