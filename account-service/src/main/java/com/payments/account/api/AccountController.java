package com.payments.account.api;

import com.payments.account.api.dto.AccountResponse;
import com.payments.account.api.dto.BalanceResponse;
import com.payments.account.api.dto.CreateAccountRequest;
import com.payments.account.api.dto.LedgerEntryResponse;
import com.payments.account.api.dto.TransferAmountRequest;
import com.payments.account.application.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Endpoints for managing customer accounts and balances")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // FR-1: Open a new bank account with RBAC owner assignment
    @PostMapping
    @Operation(summary = "Create Account", description = "Opens a new bank account with an initial positive balance")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid owner name or negative initial balance")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        UUID requestingUserId = parseUuid(userIdHeader);
        AccountResponse response = accountService.createAccount(request, requestingUserId, roleHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // List all accounts (Admin sees all; User sees only their own accounts)
    @GetMapping
    @Operation(summary = "List Accounts", description = "Retrieves accounts based on RBAC role")
    @ApiResponse(responseCode = "200", description = "List of accounts retrieved")
    public ResponseEntity<List<AccountResponse>> getAllAccounts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        UUID requestingUserId = parseUuid(userIdHeader);
        return ResponseEntity.ok(accountService.getAllAccounts(requestingUserId, roleHeader));
    }

    // FR-2: Retrieve account details with ownership check
    @GetMapping("/{id}")
    @Operation(summary = "Get Account", description = "Retrieves account metadata and current balance by UUID")
    @ApiResponse(responseCode = "200", description = "Account details found")
    @ApiResponse(responseCode = "403", description = "Forbidden: account belongs to another user")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        UUID requestingUserId = parseUuid(userIdHeader);
        return ResponseEntity.ok(accountService.getAccount(id, requestingUserId, roleHeader));
    }

    // FR-2: Retrieve account balance with ownership check
    @GetMapping("/{id}/balance")
    @Operation(summary = "Get Balance", description = "Retrieves lightweight balance and status information")
    @ApiResponse(responseCode = "200", description = "Balance fetched")
    @ApiResponse(responseCode = "403", description = "Forbidden: account belongs to another user")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        UUID requestingUserId = parseUuid(userIdHeader);
        return ResponseEntity.ok(accountService.getBalance(id, requestingUserId, roleHeader));
    }

    // FR-3: Debit an account (internal / settlement endpoint)
    @PostMapping("/{id}/debit")
    @Operation(summary = "Debit Account", description = "Atomically debits funds from the specified account")
    public ResponseEntity<AccountResponse> debit(@PathVariable UUID id, @Valid @RequestBody TransferAmountRequest request) {
        return ResponseEntity.ok(accountService.debit(id, request.amount()));
    }

    // FR-4: Credit an account (internal / settlement endpoint)
    @PostMapping("/{id}/credit")
    @Operation(summary = "Credit Account", description = "Atomically credits funds to the specified account")
    public ResponseEntity<AccountResponse> credit(@PathVariable UUID id, @Valid @RequestBody TransferAmountRequest request) {
        return ResponseEntity.ok(accountService.credit(id, request.amount()));
    }

    // Get ledger transaction history with ownership check
    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get Transactions", description = "Retrieves ledger entries and transaction history for an account")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden: account belongs to another user")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<List<LedgerEntryResponse>> getTransactions(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        UUID requestingUserId = parseUuid(userIdHeader);
        return ResponseEntity.ok(accountService.getTransactions(id, requestingUserId, roleHeader));
    }

    // Export account statement as CSV with ownership check
    @GetMapping(value = "/{id}/statement/csv", produces = "text/csv")
    @Operation(summary = "Export Statement CSV", description = "Exports an account's transaction history in CSV format")
    @ApiResponse(responseCode = "200", description = "CSV generated")
    @ApiResponse(responseCode = "403", description = "Forbidden: account belongs to another user")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<String> exportStatementCsv(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        UUID requestingUserId = parseUuid(userIdHeader);
        String csvContent = accountService.exportStatementCsv(id, requestingUserId, roleHeader);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement-" + id + ".csv\"")
                .body(csvContent);
    }

    private UUID parseUuid(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(idStr.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
