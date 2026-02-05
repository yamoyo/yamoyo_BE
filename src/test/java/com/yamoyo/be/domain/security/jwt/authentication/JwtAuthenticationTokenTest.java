package com.yamoyo.be.domain.security.jwt.authentication;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationTokenTest {

    @Test
    void getName_returnsUserIdToString() {
        // Given
        Long userId = 12345L;
        JwtTokenClaims claims = new JwtTokenClaims(userId, "test@example.com", "google");
        JwtAuthenticationToken token = JwtAuthenticationToken.authenticated(claims);

        // When
        String name = token.getName();

        // Then
        assertThat(name).isEqualTo("12345");
    }

    @Test
    void principal_isJwtTokenClaims() {
        // Given
        Long userId = 12345L;
        JwtTokenClaims claims = new JwtTokenClaims(userId, "test@example.com", "google");
        JwtAuthenticationToken token = JwtAuthenticationToken.authenticated(claims);

        // When
        Object principal = token.getPrincipal();

        // Then
        assertThat(principal).isInstanceOf(JwtTokenClaims.class);
        assertThat(((JwtTokenClaims) principal).userId()).isEqualTo(12345L);
    }
}
