package com.payments.payment.api;

import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.api.dto.PaymentResponse;
import com.payments.payment.api.dto.TimelineEntryResponse;
import com.payments.payment.application.PaymentService;
import com.payments.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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

    // Phase B: List, filter, search, and paginate payments
    @GetMapping
    @Operation(summary = "List or Search Payments", description = "Retrieves payments with optional pagination, status filter, date range, and search")
    @ApiResponse(responseCode = "200", description = "Payments retrieved")
    public ResponseEntity<?> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        if (page != null) {
            String[] sortParts = sort.split(",");
            Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
            return ResponseEntity.ok(paymentService.searchPayments(status, from, to, search, pageable));
        }

        if (status != null || from != null || to != null || (search != null && !search.trim().isEmpty())) {
            Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
            return ResponseEntity.ok(paymentService.searchPayments(status, from, to, search, pageable).getContent());
        }

        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // Phase B: Payment status timeline
    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get Payment Timeline", description = "Retrieves chronological status transition audit timeline for a payment")
    @ApiResponse(responseCode = "200", description = "Timeline retrieved")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<List<TimelineEntryResponse>> getTimeline(@PathVariable UUID id) {
        paymentService.getPayment(id);
        return ResponseEntity.ok(paymentService.getTimeline(id));
    }
}
