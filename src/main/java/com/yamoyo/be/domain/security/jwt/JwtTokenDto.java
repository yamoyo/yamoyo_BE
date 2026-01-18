package com.yamoyo.be.domain.security.jwt;

public record JwtTokenDto(
        String grantType, // Bearer
        String accessToken,
        String refreshToken,
        Long accessTokenExpiration
) {
}
