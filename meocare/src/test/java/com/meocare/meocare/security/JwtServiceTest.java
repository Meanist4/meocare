package com.meocare.meocare.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "dGVzdC1tZW9jYXJlLWp3dC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy0yNTZiaXQ=";

    @Test
    void shouldGenerateAndValidateTokenWithExpectedClaims() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);
        CustomUserDetails user = new CustomUserDetails(
                42L,
                "Dr. Emily Vance",
                "dr.vance@meocare.clinic",
                "encoded",
                RoleName.VETERINARIAN,
                UserStatus.ACTIVE
        );

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("dr.vance@meocare.clinic");
        assertThat(jwtService.extractRole(token)).isEqualTo("VETERINARIAN");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void shouldRejectTokenForInactiveOrLockedUser() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);
        CustomUserDetails activeUser = new CustomUserDetails(
                7L, "Sarah Jenkins", "sarah@oakmail.com", "encoded", RoleName.CUSTOMER, UserStatus.ACTIVE);
        CustomUserDetails lockedUser = new CustomUserDetails(
                7L, "Sarah Jenkins", "sarah@oakmail.com", "encoded", RoleName.CUSTOMER, UserStatus.LOCKED);

        String token = jwtService.generateToken(activeUser);

        assertThat(jwtService.isTokenValid(token, lockedUser)).isFalse();
    }

    @Test
    void shouldRejectExpiredAndTamperedTokens() {
        JwtService expiredJwtService = new JwtService(TEST_SECRET, -1_000L);
        String expired = expiredJwtService.generateToken(1L, "admin@meocare.clinic", RoleName.ADMIN);

        JwtService validator = new JwtService(TEST_SECRET, 60_000L);
        assertThatThrownBy(() -> validator.parseClaims(expired))
                .isInstanceOf(ExpiredJwtException.class);

        String valid = validator.generateToken(1L, "admin@meocare.clinic", RoleName.ADMIN);
        String tampered = valid.substring(0, valid.length() - 2) + "xx";
        assertThatThrownBy(() -> validator.parseClaims(tampered))
                .isInstanceOf(JwtException.class);
    }
}
