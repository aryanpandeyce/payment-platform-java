package com.payments.account.infrastructure.seed;

import com.payments.account.domain.Account;
import com.payments.account.domain.EntryType;
import com.payments.account.domain.LedgerEntry;
import com.payments.account.infrastructure.persistence.AccountRepository;
import com.payments.account.infrastructure.persistence.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// Seeds realistic Indian demo accounts matching identity-service demo users
// Idempotent: runs only if accounts table is empty
@Component
@ConditionalOnProperty(name = "seed.demo.enabled", havingValue = "true", matchIfMissing = true)
public class AccountDemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountDemoDataSeeder.class);

    // Standard User UUIDs matching IdentityDemoDataSeeder
    public static final UUID RAHUL_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID PRIYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    public static final UUID ANKIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    public static final UUID SNEHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    public static final UUID FAIZ_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
    public static final UUID KAVYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000016");
    public static final UUID VIKRAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000017");
    public static final UUID ANANYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000018");

    // Deterministic Account UUIDs
    public static final UUID RAHUL_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000011");
    public static final UUID RAHUL_ACC_2 = UUID.fromString("a1000000-0000-0000-0000-000000000021");
    public static final UUID PRIYA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000012");
    public static final UUID ANKIT_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000013");
    public static final UUID ANKIT_ACC_2 = UUID.fromString("a1000000-0000-0000-0000-000000000023");
    public static final UUID SNEHA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000014");
    public static final UUID FAIZ_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000015");
    public static final UUID KAVYA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000016");
    public static final UUID VIKRAM_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000017");
    public static final UUID ANANYA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000018");

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountDemoDataSeeder(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Accounts table is not empty; skipping account demo seed");
            return;
        }

        log.info("Seeding realistic demo accounts with initial opening ledger entries...");
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        seedAccount(RAHUL_ACC_1, "Rahul Sharma (Savings)", new BigDecimal("25000.00"), RAHUL_ID, thirtyDaysAgo);
        seedAccount(RAHUL_ACC_2, "Rahul Sharma (Salary)", new BigDecimal("60000.00"), RAHUL_ID, thirtyDaysAgo);
        seedAccount(PRIYA_ACC_1, "Priya Nair (Current)", new BigDecimal("40000.00"), PRIYA_ID, thirtyDaysAgo);
        seedAccount(ANKIT_ACC_1, "Ankit Verma (Savings)", new BigDecimal("15000.00"), ANKIT_ID, thirtyDaysAgo);
        seedAccount(ANKIT_ACC_2, "Ankit Verma (Investment)", new BigDecimal("85000.00"), ANKIT_ID, thirtyDaysAgo);
        seedAccount(SNEHA_ACC_1, "Sneha Iyer (Savings)", new BigDecimal("30000.00"), SNEHA_ID, thirtyDaysAgo);
        seedAccount(FAIZ_ACC_1, "Mohammed Faiz (Business)", new BigDecimal("120000.00"), FAIZ_ID, thirtyDaysAgo);
        seedAccount(KAVYA_ACC_1, "Kavya Reddy (Savings)", new BigDecimal("18000.00"), KAVYA_ID, thirtyDaysAgo);
        seedAccount(VIKRAM_ACC_1, "Vikram Singh (Savings)", new BigDecimal("5000.00"), VIKRAM_ID, thirtyDaysAgo);
        seedAccount(ANANYA_ACC_1, "Ananya Gupta (Savings)", new BigDecimal("50000.00"), ANANYA_ID, thirtyDaysAgo);

        log.info("Successfully seeded 10 accounts with matching ledger entries");
    }

    private void seedAccount(UUID accountId, String ownerName, BigDecimal balance, UUID ownerUserId, Instant createdAt) {
        Account account = new Account(ownerName, balance, "INR", ownerUserId);
        try {
            Field idField = Account.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, accountId);

            Field createdField = Account.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(account, createdAt);
        } catch (Exception e) {
            log.error("Failed to set account reflection properties", e);
        }

        accountRepository.save(account);

        LedgerEntry entry = new LedgerEntry(
                accountId,
                EntryType.CREDIT,
                balance,
                balance,
                "INITIAL_DEPOSIT",
                "Initial account opening balance"
        );
        try {
            Field createdField = LedgerEntry.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(entry, createdAt);
        } catch (Exception e) {
            log.error("Failed to set ledger entry createdAt", e);
        }

        ledgerEntryRepository.save(entry);
    }
}
