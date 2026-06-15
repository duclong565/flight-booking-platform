package com.deanflights.auth;

import com.deanflights.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the auth endpoints, run against real Postgres. These exercise the real
 * register -> persist -> login -> mint-JWT flow.
 */
class AuthIT extends AbstractIntegrationTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void register_returns201_withUsernameAndUserRole() throws Exception {
        String username = "reg-" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {"username":"%s","password":"password123"}
                """.formatted(username);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_withCorrectCredentials_returns200_withNonBlankToken() throws Exception {
        String username = "login-" + UUID.randomUUID().toString().substring(0, 8);
        String register = """
                {"username":"%s","password":"password123"}
                """.formatted(username);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(register))
                .andExpect(status().isCreated());

        String login = """
                {"username":"%s","password":"password123"}
                """.formatted(username);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String username = "wrongpw-" + UUID.randomUUID().toString().substring(0, 8);
        String register = """
                {"username":"%s","password":"password123"}
                """.formatted(username);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(register))
                .andExpect(status().isCreated());

        String login = """
                {"username":"%s","password":"wrong-password"}
                """.formatted(username);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isUnauthorized());
    }
}
