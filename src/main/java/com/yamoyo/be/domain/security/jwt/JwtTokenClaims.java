package com.yamoyo.be.domain.security.jwt;

public record JwtTokenClaims(
        Long userId,
        String email,
        String provider
) {
}
