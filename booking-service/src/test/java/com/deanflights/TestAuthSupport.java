package com.deanflights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test-only helper for driving the REAL auth endpoints. Logging in through
 * {@code POST /api/v1/auth/login} (rather than minting a token directly) means the integration
 * tests exercise the actual security filter chain end-to-end with genuine JWTs.
 */
public final class TestAuthSupport {

    /** Matches the seeded ADMIN from application.properties (app.admin.username/password). */
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin12345";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public TestAuthSupport(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    /** Log in and return the raw JWT (no "Bearer " prefix). */
    public String login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("token").asText();
    }

    /** Log in and return a ready-to-use "Bearer &lt;token&gt;" Authorization header value. */
    public String bearer(String username, String password) throws Exception {
        return "Bearer " + login(username, password);
    }

    /** Convenience: an admin bearer header for ADMIN-only calls. */
    public String adminBearer() throws Exception {
        return bearer(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    /**
     * Register a brand-new USER (random username), then log in. Returns a Bearer header for that
     * fresh user. The username is unique per call so tests don't collide.
     */
    public String registerAndLoginFreshUser() throws Exception {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "password123";

        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        return bearer(username, password);
    }
}
