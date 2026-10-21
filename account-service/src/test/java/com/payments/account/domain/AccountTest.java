package com.payments.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// Pure Unit Test: Yahan koi Spring Boot context (@SpringBootTest) ya Database nahi chal raha.
// Sirf core Java domain logic test ho rahi hai (ye 1 millisecond me run hoti hai).
class AccountTest {

    @Test
    @DisplayName("New account starts with the given balance")
    void shouldCreateAccountWithInitialBalance() {
        BigDecimal initialBalance = new BigDecimal("1000.00");
        Account account = new Account("Aryan Pandey", initialBalance);

        assertNotNull(account.getId());
        assertEquals("Aryan Pandey", account.getOwnerName());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
        assertNotNull(account.getCreatedAt());
    }

    @Test
    @DisplayName("Debit reduces account balance")
    void shouldDebitAmountSuccessfully() {
        Account account = new Account("Aryan Pandey", new BigDecimal("1000.00"));

        account.debit(new BigDecimal("400.00"));

        assertEquals(new BigDecimal("600.00"), account.getBalance());
    }

    @Test
    @DisplayName("Debit of more than balance throws InsufficientFundsException and leaves balance unchanged")
    void shouldThrowExceptionWhenDebitingMoreThanBalance() {
        Account account = new Account("Aryan Pandey", new BigDecimal("1000.00"));

        assertThrows(InsufficientFundsException.class, () -> {
            account.debit(new BigDecimal("1500.00"));
        });

        // Balance 1000.00 hi rehna chahiye, koi cut nahi hona chahiye
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    @Test
    @DisplayName("Debit of zero or negative amount throws InvalidAmountException")
    void shouldThrowExceptionWhenDebitingZeroOrNegative() {
        Account account = new Account("Aryan Pandey", new BigDecimal("1000.00"));

        assertThrows(InvalidAmountException.class, () -> account.debit(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> account.debit(new BigDecimal("-50.00")));
    }

    @Test
    @DisplayName("Credit increases account balance")
    void shouldCreditAmountSuccessfully() {
        Account account = new Account("Aryan Pandey", new BigDecimal("500.00"));

        account.credit(new BigDecimal("400.00"));

        assertEquals(new BigDecimal("900.00"), account.getBalance());
    }

    @Test
    @DisplayName("Credit of zero or negative amount throws InvalidAmountException")
    void shouldThrowExceptionWhenCreditingZeroOrNegative() {
        Account account = new Account("Aryan Pandey", new BigDecimal("500.00"));

        assertThrows(InvalidAmountException.class, () -> account.credit(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> account.credit(new BigDecimal("-100.00")));
    }
}
