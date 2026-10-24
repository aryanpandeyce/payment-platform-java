package com.payments.payment.controller;

import com.payments.payment.domain.IdempotencyPayloadMismatchException;
import com.payments.payment.domain.InvalidStateTransitionException;
import com.payments.payment.domain.PaymentNotFoundException;
import com.payments.payment.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// @RestControllerAdvice: Payment service exceptions ko standardized JSON responses mein map karta hai
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 409: Idempotency payload mismatch
    @ExceptionHandler(IdempotencyPayloadMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePayloadMismatch(IdempotencyPayloadMismatchException ex) {
        ErrorResponse error = new ErrorResponse("IDEMPOTENCY_PAYLOAD_MISMATCH", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 404: Payment not found
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("PAYMENT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 409: Invalid state transition
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_STATE_TRANSITION", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 400: Missing Idempotency-Key header
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        ErrorResponse error = new ErrorResponse("MISSING_IDEMPOTENCY_KEY", "Required header '" + ex.getHeaderName() + "' is missing");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 400: Bean Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse("VALIDATION_FAILED", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 400: Bad arguments (e.g. sender == receiver)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 500: Unexpected internal error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
