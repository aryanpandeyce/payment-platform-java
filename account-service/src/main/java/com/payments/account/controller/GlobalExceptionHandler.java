package com.payments.account.controller;

import com.payments.account.domain.AccountNotFoundException;
import com.payments.account.domain.InsufficientFundsException;
import com.payments.account.domain.InvalidAmountException;
import com.payments.account.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// @RestControllerAdvice: Sari REST controller exceptions ko centralized JSON format mein handle karta hai
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404: Account not found
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AccountNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("ACCOUNT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 409: Balance se zyada debit karne par
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        ErrorResponse error = new ErrorResponse("INSUFFICIENT_FUNDS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 400: Negative/zero amount
    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(InvalidAmountException ex) {
        String code = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("initial balance")
                ? "INVALID_INITIAL_BALANCE"
                : "INVALID_AMOUNT";
        ErrorResponse error = new ErrorResponse(code, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 400: Bean Validation errors (@Valid @NotNull, @NotBlank, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        boolean isInitialBalance = ex.getBindingResult().getFieldErrors().stream()
                .anyMatch(f -> "initialBalance".equals(f.getField()));

        String code = isInitialBalance ? "INVALID_INITIAL_BALANCE" : "VALIDATION_FAILED";
        ErrorResponse error = new ErrorResponse(code, details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 400: Bad argument / logic error
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 500: Unexpected internal error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
