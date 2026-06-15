package com.deanflights.auth;

import com.deanflights.auth.dto.AuthResponse;
import com.deanflights.auth.dto.LoginRequest;
import com.deanflights.auth.dto.RegisterRequest;
import com.deanflights.auth.dto.UserResponse;
import com.deanflights.common.BusinessRuleException;
import com.deanflights.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Business logic for registration and login. Register hashes the password and stores a USER;
 * login delegates credential checking to Spring's AuthenticationManager, then mints a JWT.
 * Privilege is never client-supplied — new accounts are always role USER.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Constructor injection — Spring passes these beans in automatically.
    public AuthService(UserRepository userRepository,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public UserResponse register(RegisterRequest request) {
        // Usernames are unique — reject duplicates as a business rule (422), not a 500 on the
        // DB constraint, so the client gets a clear, typed error.
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Username already taken");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password())); // store the BCrypt hash
        user.setRole(Role.USER);

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager verifies the password (via AppUserDetailsService + PasswordEncoder).
        // On bad credentials it throws an AuthenticationException, which the security layer maps to 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        // Credentials are good — load the user and mint a token for them.
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.username()));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs());
    }
}
