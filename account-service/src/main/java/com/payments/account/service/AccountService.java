package com.payments.account.service;

import com.payments.account.domain.Account;
import com.payments.account.domain.AccountNotFoundException;
import com.payments.account.domain.InvalidAmountException;
import com.payments.account.dto.AccountResponse;
import com.payments.account.dto.BalanceResponse;
import com.payments.account.dto.CreateAccountRequest;
import com.payments.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

// @Service: Spring service layer component jahan business workflows coordinate hote hain.
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    // Constructor injection: Spring container automatically AccountRepository inject karega
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // FR-1: Create account with initial balance
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Initial balance cannot be negative");
        }

        Account account = new Account(
                request.getOwnerName(),
                request.getInitialBalance(),
                request.getCurrency()
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
    // @Transactional: Database transaction boundary. Agar error aaye toh rollback ho jata hai.
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
