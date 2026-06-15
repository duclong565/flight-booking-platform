package com.deanflights.security;

import com.deanflights.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Mints and verifies JWTs. A token is a signed, self-contained statement of identity:
 * the server can trust it without a session lookup (stateless auth). The signature is an
 * HMAC over the claims using a secret only the server knows — tamper with the body and the
 * signature no longer matches.
 *
 * <p>Uses the jjwt 0.12.x API: Jwts.builder()...signWith(key) to create,
 * Jwts.parser().verifyWith(key)...parseSignedClaims(...) to read+verify.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    // Secret + lifetime come from application.properties (overridable via env in real deployments).
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // The secret is base64-encoded; decode to raw bytes and build an HMAC-SHA key from it.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getUsername())          // who the token is about
                .claim("role", user.getRole().name()) // custom claim — handy for clients/debugging
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Valid means: the signature verifies, the subject matches the loaded user, and it
     * hasn't expired. parseSignedClaims already enforces the signature and expiry (it
     * throws otherwise), so here we just confirm the subject lines up.
     */
    public boolean isValid(String token, UserDetails user) {
        String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isExpired(token);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    // --- internals ---------------------------------------------------------

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
