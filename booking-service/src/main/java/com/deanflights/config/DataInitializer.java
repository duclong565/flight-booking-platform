package com.deanflights.config;

import com.deanflights.auth.Role;
import com.deanflights.auth.User;
import com.deanflights.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a single ADMIN account on startup if it doesn't already exist, so the ADMIN-only
 * endpoints are testable out of the box. Credentials come from application.properties
 * (override via env in real deployments). We log that the admin was created — never the
 * raw password.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword) {
        return args -> {
            if (userRepository.existsByUsername(adminUsername)) {
                return; // already seeded — nothing to do
            }
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword)); // store the BCrypt hash
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Seeded ADMIN user '{}' on startup", adminUsername);
        };
    }
}
