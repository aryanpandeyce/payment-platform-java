package com.payments.identity.infrastructure.seed;

import com.payments.identity.domain.User;
import com.payments.identity.domain.UserRepository;
import com.payments.identity.domain.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// Demo seed data for Milestone 1.3 - Idempotent, runs only if users table is empty
@Component
public class IdentityDemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityDemoDataSeeder.class);

    // Standard deterministic UUIDs for demo identities
    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID RAHUL_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID PRIYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    public static final UUID ANKIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    public static final UUID SNEHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    public static final UUID FAIZ_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
    public static final UUID KAVYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000016");
    public static final UUID VIKRAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000017");
    public static final UUID ANANYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000018");

    private final UserRepository userRepository;
    private final String adminPassword;
    private final String userPassword;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public IdentityDemoDataSeeder(
            UserRepository userRepository,
            @Value("${seed.admin-password:AryanAdmin#2026}") String adminPassword,
            @Value("${seed.user-password:DemoUser#2026}") String userPassword) {
        this.userRepository = userRepository;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users table is not empty; skipping demo identity seed");
            return;
        }

        log.info("Seeding demo identities into identity_db...");

        String adminHash = passwordEncoder.encode(adminPassword);
        String userHash = passwordEncoder.encode(userPassword);

        // 1. Admin: Aryan Pandey
        User admin = User.createWithId(
                ADMIN_ID,
                "aryan.pandey",
                "aryan.pandey@apexpay.demo",
                "Aryan Pandey",
                adminHash,
                UserRole.ADMIN
        );

        // 2. 8 Indian Users
        User rahul = User.createWithId(RAHUL_ID, "rahul.sharma", "rahul.sharma@apexpay.demo", "Rahul Sharma", userHash, UserRole.USER);
        User priya = User.createWithId(PRIYA_ID, "priya.nair", "priya.nair@apexpay.demo", "Priya Nair", userHash, UserRole.USER);
        User ankit = User.createWithId(ANKIT_ID, "ankit.verma", "ankit.verma@apexpay.demo", "Ankit Verma", userHash, UserRole.USER);
        User sneha = User.createWithId(SNEHA_ID, "sneha.iyer", "sneha.iyer@apexpay.demo", "Sneha Iyer", userHash, UserRole.USER);
        User faiz = User.createWithId(FAIZ_ID, "mohammed.faiz", "mohammed.faiz@apexpay.demo", "Mohammed Faiz", userHash, UserRole.USER);
        User kavya = User.createWithId(KAVYA_ID, "kavya.reddy", "kavya.reddy@apexpay.demo", "Kavya Reddy", userHash, UserRole.USER);

        // Vikram Singh: 5 failed attempts, LOCKED to demonstrate account lockout flow
        User vikram = User.createWithId(VIKRAM_ID, "vikram.singh", "vikram.singh@apexpay.demo", "Vikram Singh", userHash, UserRole.USER);
        for (int i = 0; i < 5; i++) {
            vikram.recordFailedLogin();
        }

        User ananya = User.createWithId(ANANYA_ID, "ananya.gupta", "ananya.gupta@apexpay.demo", "Ananya Gupta", userHash, UserRole.USER);

        userRepository.saveAll(List.of(admin, rahul, priya, ankit, sneha, faiz, kavya, vikram, ananya));
        log.info("Successfully seeded 9 users (1 ADMIN, 8 USERs, 1 LOCKED)");
    }
}
