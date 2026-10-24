package com.payments.account.repository;

import com.payments.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// @Repository: Spring Data JPA interface.
// Spring runtime pe SQL queries (SELECT, INSERT, UPDATE) khud generate kar deta hai.
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
}
