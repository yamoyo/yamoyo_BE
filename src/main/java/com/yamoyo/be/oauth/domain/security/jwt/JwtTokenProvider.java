package com.yamoyo.be.oauth.domain.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱 담당 클래스
 *
 * Role:
 * - OAuth2 로그인 성공 후 JWT Access/Refresh Token 생성
 * - API 요청 시 토큰 유효성 검증
 * - 토큰에서 사용자 정보(Claims) 추출
 *
 * Complexity/Rationale:
 * - jjwt 0.12.x 버전 사용 (최신 API)
 * - HMAC-SHA256 알고리즘으로 서명
 * - Access Token: 10분 (짧은 수명)
 * - Refresh Token: 7일 (긴 수명, DB에 저장하여 관리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    /**
     * 초기화: Secret Key를 HMAC-SHA 알고리즘용 SecretKey 객체로 변환
     *
     * Role:
     * - application.yml의 Base64 인코딩된 secret-key를 디코딩
     * - HMAC-SHA256 알고리즘용 SecretKey 객체 생성
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.secretKey());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 사용자 정보를 기반으로 Access Token + Refresh Token 생성
     *
     * Role:
     * - OAuth2 로그인 성공 시 호출
     * - Access Token: API 인증용 (짧은 수명)
     * - Refresh Token: Access Token 재발급용 (긴 수명)
     *
     * @param userId 사용자 ID
     * @param email 이메일
     * @param role 권한 (USER, ADMIN 등)
     * @param provider OAuth2 Provider (google, kakao)
     * @return JwtTokenDto (Access Token, Refresh Token, 만료시간)
     */
    public JwtTokenDto generateToken(Long userId, String email, String role, String provider) {
        Date now = new Date();
        Date accessTokenExpiry = new Date(now.getTime() + jwtProperties.accessTokenExpiration());
        Date refreshTokenExpiry = new Date(now.getTime() + jwtProperties.refreshTokenExpiration());

        // Access Token 생성 (10분)
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))  // 사용자 ID를 subject에 저장
                .claim("email", email)
                .claim("role", role)
                .claim("provider", provider)
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiration(accessTokenExpiry)
                .signWith(key)  // HMAC-SHA256으로 서명
                .compact();

        // Refresh Token 생성 (7일)
        String refreshToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("provider", provider)
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiration(refreshTokenExpiry)
                .signWith(key)
                .compact();

        return new JwtTokenDto("Bearer", accessToken, refreshToken, jwtProperties.accessTokenExpiration());
    }

    /**
     * 토큰 유효성 검증
     *
     * Role:
     * - JwtAuthenticationFilter에서 호출
     * - 서명 검증, 만료 확인
     *
     * @param token JWT 토큰
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 Claims (사용자 정보) 추출
     *
     * Role:
     * - 유효한 토큰에서 사용자 정보 추출
     * - JwtAuthenticationFilter에서 SecurityContext에 저장할 때 사용
     *
     * @param token JWT 토큰
     * @return JwtTokenClaims (userId, email, role, provider)
     */
    public JwtTokenClaims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtTokenClaims(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class),
                claims.get("provider", String.class)
        );
    }

    /**
     * 만료된 토큰에서 Claims 추출 (Refresh Token 갱신 시 사용)
     *
     * Role:
     * - Access Token이 만료되었지만 Refresh Token은 유효한 경우
     * - 만료된 Access Token에서 사용자 정보를 추출하여 새 토큰 발급
     *
     * @param token 만료된 JWT 토큰
     * @return JwtTokenClaims (userId, email, role, provider)
     */
    public JwtTokenClaims parseClaimsFromExpiredToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new JwtTokenClaims(
                    Long.parseLong(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("role", String.class),
                    claims.get("provider", String.class)
            );
        } catch (ExpiredJwtException e) {
            // 만료된 토큰의 경우 ExpiredJwtException에서 Claims 추출
            Claims claims = e.getClaims();
            return new JwtTokenClaims(
                    Long.parseLong(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("role", String.class),
                    claims.get("provider", String.class)
            );
        }
    }
}
