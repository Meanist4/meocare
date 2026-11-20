package com.meocare.meocare.security;

import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = buildSigningKey(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new IllegalStateException("Cannot generate token for user without role");
        }
        return generateToken(user.getId(), user.getEmail(), user.getRole().getName());
    }

    public String generateToken(CustomUserDetails userDetails) {
        if (userDetails.getRole() == null) {
            throw new IllegalStateException("Cannot generate token for user without role");
        }
        return generateToken(userDetails.getId(), userDetails.getEmail(), userDetails.getRole());
    }

    public String generateToken(Long userId, String email, RoleName role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        String normalizedEmail = email.trim().toLowerCase();

        return Jwts.builder()
                .subject(normalizedEmail)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_EMAIL, normalizedEmail)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(Claims claims) {
        Object value = claims.get(CLAIM_USER_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            return Long.parseLong(str);
        }
        return null;
    }

    public Long extractUserId(String token) {
        return extractUserId(parseClaims(token));
    }

    public String extractEmail(Claims claims) {
        String email = claims.get(CLAIM_EMAIL, String.class);
        return email != null ? email : claims.getSubject();
    }

    public String extractEmail(String token) {
        return extractEmail(parseClaims(token));
    }

    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public String extractRole(String token) {
        return extractRole(parseClaims(token));
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token, CustomUserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            Long tokenUserId = extractUserId(claims);
            String tokenEmail = extractEmail(claims);
            Date expiration = claims.getExpiration();
            return userDetails.isActive()
                    && expiration != null
                    && expiration.after(new Date())
                    && tokenUserId != null
                    && tokenUserId.equals(userDetails.getId())
                    && tokenEmail != null
                    && tokenEmail.equalsIgnoreCase(userDetails.getEmail());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 256 bits (32 bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
