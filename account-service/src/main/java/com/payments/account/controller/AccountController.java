package com.payments.account.controller;

import com.payments.account.dto.AccountResponse;
import com.payments.account.dto.BalanceResponse;
import com.payments.account.dto.CreateAccountRequest;
import com.payments.account.dto.TransferAmountRequest;
import com.payments.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// @RestController: Spring ko batata hai ki ye class HTTP JSON API endpoints handle karegi.
// @RequestMapping: Is controller ke sabhi URLs "/api/accounts" se start honge.
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // FR-1: Create a new account
    // @PostMapping: HTTP POST request handle karta hai
    // @ResponseStatus(HttpStatus.CREATED): 201 status code return karta hai
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    // FR-2: Get account by ID
    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable("id") UUID id) {
        return accountService.getAccount(id);
    }

    // FR-2: Get balance only
    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(@PathVariable("id") UUID id) {
        return accountService.getBalance(id);
    }

    // FR-3: Internal debit
    @PostMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable("id") UUID id, @Valid @RequestBody TransferAmountRequest request) {
        return accountService.debit(id, request.getAmount());
    }

    // FR-4: Internal credit
    @PostMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable("id") UUID id, @Valid @RequestBody TransferAmountRequest request) {
        return accountService.credit(id, request.getAmount());
    }
}
