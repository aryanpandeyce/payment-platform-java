package com.payments.account.application;

import com.payments.account.api.dto.AccountResponse;
import com.payments.account.api.dto.BalanceResponse;
import com.payments.account.api.dto.CreateAccountRequest;
import com.payments.account.api.dto.LedgerEntryResponse;
import com.payments.account.domain.Account;
import com.payments.account.domain.AccountNotFoundException;
import com.payments.account.domain.EntryType;
import com.payments.account.domain.InvalidAmountException;
import com.payments.account.domain.LedgerEntry;
import com.payments.account.infrastructure.persistence.AccountRepository;
import com.payments.account.infrastructure.persistence.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // Get all accounts
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::from)
                .toList();
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

    // Atomic Transfer with reference ID
    @Transactional
    public void transfer(UUID senderId, UUID receiverId, BigDecimal amount, String referenceId) {
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

        ledgerEntryRepository.save(new LedgerEntry(
                sender.getId(),
                EntryType.DEBIT,
                amount,
                sender.getBalance(),
                referenceId,
                "Transfer to " + receiver.getOwnerName()
        ));

        ledgerEntryRepository.save(new LedgerEntry(
                receiver.getId(),
                EntryType.CREDIT,
                amount,
                receiver.getBalance(),
                referenceId,
                "Transfer from " + sender.getOwnerName()
        ));
    }

    @Transactional
    public void transfer(UUID senderId, UUID receiverId, BigDecimal amount) {
        transfer(senderId, receiverId, amount, null);
    }

    // Phase B: Get transaction history (ledger entries)
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getTransactions(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }

    // Phase B: Statement CSV export
    @Transactional(readOnly = true)
    public String exportStatementCsv(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        StringBuilder sb = new StringBuilder();
        sb.append("TransactionId,AccountId,OwnerName,Timestamp,Type,Amount,BalanceAfter,ReferenceId,Description\n");
        for (LedgerEntry entry : entries) {
            sb.append(entry.getId()).append(",")
                    .append(entry.getAccountId()).append(",")
                    .append(escapeCsv(account.getOwnerName())).append(",")
                    .append(entry.getCreatedAt()).append(",")
                    .append(entry.getEntryType()).append(",")
                    .append(entry.getAmount()).append(",")
                    .append(entry.getBalanceAfter()).append(",")
                    .append(entry.getReferenceId() == null ? "" : escapeCsv(entry.getReferenceId())).append(",")
                    .append(entry.getDescription() == null ? "" : escapeCsv(entry.getDescription())).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
