package com.lukuboost.electricityloan.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "dev-secret-key-change-in-production-minimum-256-bits-long-key-here",
                15L,
                7L,
                "luku-boost"
        );
    }

    @Test
    void generateAccessToken_shouldProduceValidToken() {
        Long userId = 1L;
        Set<String> permissions = Set.of("LOAN_VIEW", "CUSTOMER_VIEW");

        String token = jwtService.generateAccessToken(userId, "admin", permissions);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getUserId(token)).isEqualTo(userId);
        assertThat(jwtService.getUsername(token)).isEqualTo("admin");
        assertThat(jwtService.getPermissions(token)).containsExactlyInAnyOrderElementsOf(permissions);
        assertThat(jwtService.getTokenType(token)).isEqualTo("ACCESS");
    }

    @Test
    void generateRefreshToken_shouldProduceValidToken() {
        Long userId = 1L;

        String token = jwtService.generateRefreshToken(userId);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getUserId(token)).isEqualTo(userId);
        assertThat(jwtService.getTokenType(token)).isEqualTo("REFRESH");
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertThat(jwtService.validateToken("invalid.token.here")).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
        assertThat(jwtService.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForTamperedToken() {
        Long userId = 1L;
        String token = jwtService.generateAccessToken(userId, "admin", Set.of());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }
}
