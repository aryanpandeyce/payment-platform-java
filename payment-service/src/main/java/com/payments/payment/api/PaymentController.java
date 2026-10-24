package com.payments.payment.api;

import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.api.dto.PaymentResponse;
import com.payments.payment.application.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints for initiating idempotent transfers and querying payment status lifecycle")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // FR-5: Initiate payment with required Idempotency-Key header
    @PostMapping
    @Operation(summary = "Initiate Payment", description = "Initiates an idempotent money transfer transaction between two bank accounts")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Unique client-generated idempotency key (UUID)", example = "3f2e1a0b-4c5d-6e7f-8a9b-0c1d2e3f4a5b")
    @ApiResponse(responseCode = "201", description = "Payment created and transfer initiated")
    @ApiResponse(responseCode = "200", description = "Idempotent replay: exact same transfer previously processed")
    @ApiResponse(responseCode = "400", description = "Missing idempotency key, same sender/receiver, or invalid amount")
    @ApiResponse(responseCode = "409", description = "Idempotency Conflict: same key reused with different transfer parameters")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {

        PaymentService.InitiationResult result = paymentService.initiatePayment(idempotencyKey, request);
        HttpStatus status = result.isNew() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());

    }

    // FR-7: Query payment by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get Payment by ID", description = "Retrieves the real-time transaction state and lifecycle status")
    @ApiResponse(responseCode = "200", description = "Payment transaction found")
    @ApiResponse(responseCode = "404", description = "Payment ID not found")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    // FR-7: List all payments
    @GetMapping
    @Operation(summary = "List Payments", description = "Retrieves list of all payment transactions")
    @ApiResponse(responseCode = "200", description = "List of payments retrieved")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
