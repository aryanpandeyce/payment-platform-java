package com.payments.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

// @Entity: Hibernate ko batata hai ki ye class database ki table se map hogi.
// @Table: DB table ka naam "accounts" specify kar rahe hain (PostgreSQL me "account" reserved word ho sakta hai).
@Entity
@Table(name = "accounts")
public class Account {

    // @Id: Ye primary key hai. UUID use kar rahe hain taaki unique identifier distributed system me bina DB dependency ke ban sake.
    // updatable = false: Ek baar ID assign ho gayi toh kabhi update nahi honi chahiye.
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    // precision = 19, scale = 2: Maximum 19 digits aur 2 decimal places (e.g. 1000.50). Floating point errors se bachne ke liye BigDecimal.
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    // @Version: Optimistic Locking ke liye!
    // Real-life Analogy: Wikipedia page edit token.
    // Agar 2 log ek sath same account se paise nikalne aate hain, toh dono ko version=1 milega.
    // Jo pehle save karega uska version=2 ho jayega. Doosre wale ka save fail ho jayega (OptimisticLockException)
    // kyunki uska expected version 1 tha par DB me version 2 ho chuka hai. Isse race conditions aur double-spending prevent hoti hai bina database lock lagaye.
    @Version
    private Long version;

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA/Hibernate ko reflection se object banane ke liye default no-args constructor chahiye hota hai.
    // Isko protected rakha hai taaki bahar ka code bina valid data ke empty Account na bana sake.
    protected Account() {
    }

    // Public constructor: default currency INR
    public Account(String ownerName, BigDecimal initialBalance) {
        this(ownerName, initialBalance, "INR", null);
    }

    // Overloaded constructor: currency customize karne ke liye
    public Account(String ownerName, BigDecimal initialBalance, String currency) {
        this(ownerName, initialBalance, currency, null);
    }

    // Overloaded constructor: RBAC user ownership ke liye
    public Account(String ownerName, BigDecimal initialBalance, String currency, UUID ownerUserId) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be empty");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Initial balance cannot be negative or null");
        }

        this.id = UUID.randomUUID();
        this.ownerName = ownerName.trim();
        // Scale 2 decimal places enforce kar rahe hain banker's rounding ke sath
        this.balance = initialBalance.setScale(2, RoundingMode.HALF_EVEN);
        this.currency = (currency == null || currency.trim().isEmpty()) ? "INR" : currency.trim().toUpperCase();
        this.status = "ACTIVE";
        this.ownerUserId = ownerUserId;
        this.createdAt = Instant.now();
    }

    // Business Logic Method: Debit
    // Notice: Ye logic kisi Service class me nahi, Entity ke andar hai (Rich Domain Model).
    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Debit amount must be strictly greater than zero");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Available balance: " + this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    // Business Logic Method: Credit
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Credit amount must be strictly greater than zero");
        }
        this.balance = this.balance.add(amount);
    }

    // Plain Java Getters (No Lombok)
    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }
}
