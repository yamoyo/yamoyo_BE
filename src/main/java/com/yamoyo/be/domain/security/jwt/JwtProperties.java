package com.yamoyo.be.domain.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 Properties
 *
 * Role:
 * - application.yml의 jwt.* 설정을 타입 안전하게 바인딩
 * - record 타입으로 불변성 보장
 *
 * Complexity/Rationale:
 * - @ConfigurationProperties를 사용하여 yml 설정 자동 바인딩
 * - OAuthApplication에서 @ConfigurationPropertiesScan으로 자동 등록
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secretKey,              // JWT 서명에 사용할 비밀키 (Base64 인코딩)
        long accessTokenExpiration,    // Access Token 만료 시간 (ms)
        long refreshTokenExpiration,   // Refresh Token 만료 시간 (ms)
        String issuer                  // 토큰 발행자
) {
}
