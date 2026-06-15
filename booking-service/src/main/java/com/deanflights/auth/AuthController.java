package com.deanflights.auth;

import com.deanflights.auth.dto.AuthResponse;
import com.deanflights.auth.dto.LoginRequest;
import com.deanflights.auth.dto.RegisterRequest;
import com.deanflights.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP layer for authentication. Only concerns: routing, status codes, JSON in/out, and
 * triggering validation (@Valid). It never returns the entity directly — always a DTO.
 * Both routes are public (see SecurityConfig); everything else needs the token they issue.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
