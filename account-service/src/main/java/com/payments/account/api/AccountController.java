package com.payments.account.api;

import com.payments.account.api.dto.AccountResponse;
import com.payments.account.api.dto.BalanceResponse;
import com.payments.account.api.dto.CreateAccountRequest;
import com.payments.account.api.dto.TransferAmountRequest;
import com.payments.account.application.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Endpoints for managing customer accounts and balances")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // FR-1: Open a new bank account
    @PostMapping
    @Operation(summary = "Create Account", description = "Opens a new bank account with an initial positive balance")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid owner name or negative initial balance")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // FR-2: Retrieve account details
    @GetMapping("/{id}")
    @Operation(summary = "Get Account", description = "Retrieves account metadata and current balance by UUID")
    @ApiResponse(responseCode = "200", description = "Account details found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    // FR-2: Retrieve account balance
    @GetMapping("/{id}/balance")
    @Operation(summary = "Get Balance", description = "Retrieves lightweight balance and status information")
    @ApiResponse(responseCode = "200", description = "Balance fetched")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    // FR-3: Debit an account
    @PostMapping("/{id}/debit")
    @Operation(summary = "Debit Account", description = "Atomically debits funds from the specified account")
    @ApiResponse(responseCode = "200", description = "Debit successful")
    @ApiResponse(responseCode = "400", description = "Invalid or negative debit amount")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "409", description = "Insufficient funds")
    public ResponseEntity<AccountResponse> debit(@PathVariable UUID id, @Valid @RequestBody TransferAmountRequest request) {
        return ResponseEntity.ok(accountService.debit(id, request.amount()));
    }

    // FR-4: Credit an account
    @PostMapping("/{id}/credit")
    @Operation(summary = "Credit Account", description = "Atomically credits funds to the specified account")
    @ApiResponse(responseCode = "200", description = "Credit successful")
    @ApiResponse(responseCode = "400", description = "Invalid or negative credit amount")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountResponse> credit(@PathVariable UUID id, @Valid @RequestBody TransferAmountRequest request) {
        return ResponseEntity.ok(accountService.credit(id, request.amount()));
    }
}
