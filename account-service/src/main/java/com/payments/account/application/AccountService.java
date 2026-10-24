package com.payments.account.application;

import com.payments.account.api.dto.AccountResponse;
import com.payments.account.api.dto.BalanceResponse;
import com.payments.account.api.dto.CreateAccountRequest;
import com.payments.account.domain.Account;
import com.payments.account.domain.AccountNotFoundException;
import com.payments.account.domain.InvalidAmountException;
import com.payments.account.infrastructure.persistence.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

// Application Service coordinating domain operations and database persistence
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // FR-1: Create account with initial balance
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (request.initialBalance() != null && request.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Initial balance cannot be negative");
        }

        Account account = new Account(
                request.ownerName(),
                request.initialBalance(),
                request.currency()
        );

        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    // FR-2: Get account details by ID
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountResponse.from(account);
    }

    // FR-2: Get account balance
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return new BalanceResponse(account.getId(), account.getBalance(), account.getCurrency(), account.getStatus());
    }

    // FR-3: Debit funds atomically
    @Transactional
    public AccountResponse debit(UUID id, BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.debit(amount);
        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    // FR-4: Credit funds atomically
    @Transactional
    public AccountResponse credit(UUID id, BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.credit(amount);
        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    // Atomic Transfer (Sender debit + Receiver credit in one single database transaction)
    @Transactional
    public void transfer(UUID senderId, UUID receiverId, BigDecimal amount) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Sender and receiver account cannot be the same");
        }

        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found: " + senderId));
        Account receiver = accountRepository.findById(receiverId)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found: " + receiverId));

        sender.debit(amount);
        receiver.credit(amount);

        accountRepository.save(sender);
        accountRepository.save(receiver);
    }
}
