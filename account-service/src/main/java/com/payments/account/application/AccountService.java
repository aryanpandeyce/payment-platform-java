package com.payments.account.application;

import com.payments.account.api.dto.AccountResponse;
import com.payments.account.api.dto.BalanceResponse;
import com.payments.account.api.dto.CreateAccountRequest;
import com.payments.account.api.dto.LedgerEntryResponse;
import com.payments.account.domain.*;
import com.payments.account.infrastructure.persistence.AccountRepository;
import com.payments.account.infrastructure.persistence.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

// Application Service coordinating domain operations, persistence, and audit ledger entries
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    // FR-1: Create account with initial balance and log opening ledger entry
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, UUID requestingUserId, String role) {
        if (request.initialBalance() != null && request.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Initial balance cannot be negative");
        }

        UUID ownerId = request.ownerUserId() != null ? request.ownerUserId() : requestingUserId;

        Account account = new Account(
                request.ownerName(),
                request.initialBalance(),
                request.currency(),
                ownerId
        );

        Account saved = accountRepository.save(account);

        if (saved.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            ledgerEntryRepository.save(new LedgerEntry(
                    saved.getId(),
                    EntryType.CREDIT,
                    saved.getBalance(),
                    saved.getBalance(),
                    "INITIAL_DEPOSIT",
                    "Initial account opening balance"
            ));
        }

        return AccountResponse.from(saved);
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        return createAccount(request, null, null);
    }

    // FR-21: List accounts with RBAC (Admin sees all; User sees only their own accounts)
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts(UUID requestingUserId, String role) {
        if ("ADMIN".equalsIgnoreCase(role) || requestingUserId == null) {
            return accountRepository.findAll()
                    .stream()
                    .map(AccountResponse::from)
                    .toList();
        }
        return accountRepository.findByOwnerUserId(requestingUserId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return getAllAccounts(null, null);
    }

    // FR-2 & FR-21: Get account details by ID with RBAC check
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id, UUID requestingUserId, String role) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        validateOwnership(account, requestingUserId, role);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        return getAccount(id, null, null);
    }

    // FR-2 & FR-21: Get account balance with RBAC check
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID id, UUID requestingUserId, String role) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        validateOwnership(account, requestingUserId, role);
        return new BalanceResponse(account.getId(), account.getBalance(), account.getCurrency(), account.getStatus());
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID id) {
        return getBalance(id, null, null);
    }

    // FR-3: Debit funds atomically and record ledger entry
    @Transactional
    public AccountResponse debit(UUID id, BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.debit(amount);
        Account saved = accountRepository.save(account);

        ledgerEntryRepository.save(new LedgerEntry(
                saved.getId(),
                EntryType.DEBIT,
                amount,
                saved.getBalance(),
                null,
                "Direct debit"
        ));

        return AccountResponse.from(saved);
    }

    // FR-4: Credit funds atomically and record ledger entry
    @Transactional
    public AccountResponse credit(UUID id, BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.credit(amount);
        Account saved = accountRepository.save(account);

        ledgerEntryRepository.save(new LedgerEntry(
                saved.getId(),
                EntryType.CREDIT,
                amount,
                saved.getBalance(),
                null,
                "Direct credit"
        ));

        return AccountResponse.from(saved);
    }

    // Coordinated settlement transfer between two accounts
    @Transactional
    public void transfer(UUID senderId, UUID receiverId, BigDecimal amount, String referenceId) {
        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new AccountNotFoundException(senderId));
        Account receiver = accountRepository.findById(receiverId)
                .orElseThrow(() -> new AccountNotFoundException(receiverId));

        sender.debit(amount);
        accountRepository.save(sender);

        ledgerEntryRepository.save(new LedgerEntry(
                sender.getId(),
                EntryType.DEBIT,
                amount,
                sender.getBalance(),
                referenceId,
                "Transfer to Account " + receiver.getId()
        ));

        receiver.credit(amount);
        accountRepository.save(receiver);

        ledgerEntryRepository.save(new LedgerEntry(
                receiver.getId(),
                EntryType.CREDIT,
                amount,
                receiver.getBalance(),
                referenceId,
                "Transfer from Account " + sender.getId()
        ));
    }

    // Get ledger transaction history with RBAC check
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getTransactions(UUID id, UUID requestingUserId, String role) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        validateOwnership(account, requestingUserId, role);

        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(id)
                .stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getTransactions(UUID id) {
        return getTransactions(id, null, null);
    }

    // Export account statement as CSV with RBAC check
    @Transactional(readOnly = true)
    public String exportStatementCsv(UUID id, UUID requestingUserId, String role) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        validateOwnership(account, requestingUserId, role);

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(id);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("TransactionId,AccountId,OwnerName,Timestamp,Type,Amount,BalanceAfter,ReferenceId,Description");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        for (LedgerEntry e : entries) {
            pw.printf("%s,%s,\"%s\",%s,%s,%s,%s,\"%s\",\"%s\"%n",
                    e.getId(),
                    e.getAccountId(),
                    account.getOwnerName(),
                    formatter.format(e.getCreatedAt()),
                    e.getEntryType(),
                    e.getAmount().toPlainString(),
                    e.getBalanceAfter().toPlainString(),
                    e.getReferenceId() != null ? e.getReferenceId() : "",
                    e.getDescription() != null ? e.getDescription() : ""
            );
        }

        pw.flush();
        return sw.toString();
    }

    @Transactional(readOnly = true)
    public String exportStatementCsv(UUID id) {
        return exportStatementCsv(id, null, null);
    }

    // Invariant FR-21: USER role can only access accounts matching their X-User-Id
    private void validateOwnership(Account account, UUID requestingUserId, String role) {
        if ("ADMIN".equalsIgnoreCase(role) || requestingUserId == null) {
            return;
        }
        if (account.getOwnerUserId() != null && !account.getOwnerUserId().equals(requestingUserId)) {
            throw new ForbiddenException("Access denied: You do not have permission to view or manage this account");
        }
    }
}
