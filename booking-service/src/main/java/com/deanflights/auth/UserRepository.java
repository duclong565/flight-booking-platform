package com.deanflights.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data derives these queries from the method names — no @Query needed.
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
