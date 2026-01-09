package com.yamoyo.be.oauth.domain.security.oauth.handler;

import com.example.oauth.domain.security.jwt.JwtTokenDto;
import com.example.oauth.domain.security.jwt.JwtTokenProvider;
import com.example.oauth.domain.security.oauth.CustomOAuth2User;
import com.example.oauth.domain.security.refreshtoken.RefreshToken;
import com.example.oauth.domain.security.refreshtoken.RefreshTokenRepository;
import com.example.oauth.domain.user.entity.User;
import com.example.oauth.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OAuth2 Authentication Success Handler
 *
 * Role:
 * - OAuth2 로그인 성공 시 JWT 토큰을 발급하고 프론트엔드로 리다이렉트
 * - Google, Kakao 등 OAuth2 로그인 완료 후 JWT 기반 인증으로 전환
 *
 * Complexity/Rationale:
 * 1. OAuth2 로그인 플로우:
 *    - 사용자가 OAuth2 Provider(Google, Kakao)로 로그인 성공
 *    - Spring Security가 CustomOAuth2UserService로 사용자 정보 로드
 *    - 이 Handler가 호출되어 JWT 토큰 발급
 *    - 프론트엔드로 리다이렉트하며 토큰을 쿼리 파라미터로 전달
 *
 * 2. JWT 토큰 발급:
 *    - DB에서 User 엔티티 조회 (provider + providerId로 식별)
 *    - JwtTokenProvider로 Access Token + Refresh Token 생성
 *    - Refresh Token을 DB에 저장 (재발급 및 로그아웃 시 사용)
 *
 * 3. 프론트엔드 연동:
 *    - 리다이렉트 URL에 토큰을 쿼리 파라미터로 첨부
 *    - 프론트엔드는 토큰을 받아서 localStorage에 저장
 *    - 이후 API 요청 시 Authorization: Bearer {accessToken} 헤더로 인증
 *
 * 4. Refresh Token 관리:
 *    - userId별로 하나의 Refresh Token만 유지
 *    - 중복 로그인 시 기존 Refresh Token 업데이트
 *    - 로그아웃 시 DB에서 삭제하여 무효화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * OAuth2 로그인 성공 리다이렉트 URL
     * - 프론트엔드 URL (예: http://localhost:3000/oauth/callback)
     * - application.yml에서 설정
     */
    @Value("${oauth2.redirect-uri:http://localhost/oauth/callback}")
    private String redirectUri;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    /**
     * OAuth2 로그인 성공 시 호출되는 메서드
     *
     * Role:
     * - JWT 토큰 발급 및 Refresh Token DB 저장
     * - HTML Form Auto-Submit으로 프론트엔드에 POST 요청 (Access Token을 body에 포함)
     *
     * Complexity/Rationale:
     * 1. 사용자 정보 추출:
     *    - OAuth2AuthenticationToken에서 CustomOAuth2User 추출
     *    - registrationId(provider)와 providerId 추출
     *
     * 2. User 엔티티 조회:
     *    - provider + providerId로 DB에서 User 조회
     *    - CustomOAuth2UserService에서 이미 저장한 사용자
     *
     * 3. JWT 토큰 생성:
     *    - JwtTokenProvider로 Access Token + Refresh Token 생성
     *    - Access Token: 10분, Refresh Token: 7일
     *
     * 4. Refresh Token 저장:
     *    - DB에 userId별로 Refresh Token 저장
     *    - HttpOnly Cookie로 전달 (XSS 방어)
     *
     * 5. Access Token 전달 (보안 강화):
     *    - 쿼리 파라미터 대신 POST body로 전달
     *    - HTML Form Auto-Submit 방식 사용
     *    - 브라우저 히스토리/로그에 토큰 노출 방지
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authentication OAuth2 인증 정보 (CustomOAuth2User 포함)
     * @throws IOException HTML 응답 실패 시
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 1. OAuth2 인증 정보 추출
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        CustomOAuth2User oAuth2User = (CustomOAuth2User) oAuth2Token.getPrincipal();

        // 2. Provider 정보 추출
        // registrationId: "google", "kakao" 등
        String provider = oAuth2Token.getAuthorizedClientRegistrationId();

        // providerId: OAuth2User.getName()은 nameAttributeKey에 해당하는 값 반환
        // Google: "sub" 값, Kakao: "id" 값
        String providerId = oAuth2User.getName();

        log.info("OAuth2 로그인 성공 - Provider: {}, ProviderId: {}", provider, providerId);

        // 3. DB에서 User 엔티티 조회
        // CustomOAuth2UserService에서 이미 저장한 사용자
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalStateException(
                        "OAuth2 로그인 성공했으나 DB에 사용자가 없습니다. Provider: " + provider + ", ProviderId: " + providerId));

        // 4. JWT 토큰 생성
        // userId, email, role, provider를 포함하는 Access Token + Refresh Token 생성
        JwtTokenDto jwtToken = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getProvider()
        );

        log.info("JWT 토큰 발급 완료 - UserId: {}, Email: {}", user.getId(), user.getEmail());

        // 5. Refresh Token DB 저장
        // userId별로 하나의 Refresh Token만 유지 (중복 로그인 시 기존 토큰 업데이트)
        saveRefreshToken(user.getId(), jwtToken.refreshToken(), refreshTokenExpiration);

        // 6. Refresh Token을 HttpOnly Cookie에 설정 (XSS 방어)
        addRefreshTokenCookie(response, jwtToken.refreshToken(), refreshTokenExpiration);

        // 7. 프론트엔드로 리다이렉트
        // Access Token은 전달하지 않고, 프론트엔드에서 쿠키에 저장된 Refresh Token을 사용하여 /api/auth/refresh 호출
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    /**
     * Refresh Token을 DB에 저장 또는 업데이트
     *
     * Role:
     * - userId별로 하나의 Refresh Token만 유지
     * - 기존 Refresh Token이 있으면 업데이트, 없으면 새로 생성
     *
     * Complexity/Rationale:
     * - userId를 unique key로 설정하여 중복 로그인 시 자동으로 갱신
     * - Refresh Token 탈취 방지: DB에 저장하여 서버에서 검증 가능
     * - 로그아웃 시 DB에서 삭제하여 무효화 가능
     *
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token 문자열
     * @param refreshTokenExpiration Refresh Token 만료 시간 (ms)
     */
    private void saveRefreshToken(Long userId, String refreshToken, Long refreshTokenExpiration) {
        // 만료 시간 계산: 현재 시간 + refreshTokenExpiration(ms)
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        // 기존 Refresh Token이 있으면 업데이트, 없으면 새로 생성
        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        // 기존 토큰 업데이트
                        existingToken -> existingToken.updateToken(refreshToken, expiryDate),
                        // 새 토큰 생성
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(userId)
                                .token(refreshToken)
                                .expiryDate(expiryDate)
                                .build())
                );

        log.debug("Refresh Token 저장 완료 - UserId: {}, ExpiryDate: {}", userId, expiryDate);
    }

    /**
     * Refresh Token을 HttpOnly Cookie로 설정
     * Role: XSS 공격 방지를 위해 Refresh Token을 쿠키에 담아 전달
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, Long refreshTokenExpiration) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가 (XSS 방어)
        cookie.setSecure(false); // HTTPS 적용 시 true로 변경 필요 (로컬 개발 환경 고려하여 false)
        cookie.setPath("/"); // 모든 경로에서 쿠키 전송
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000)); // 쿠키 만료 시간 (초 단위)
        
        // SameSite 설정은 Spring Boot 버전에 따라 별도 설정이 필요할 수 있음
        // response.addHeader("Set-Cookie", String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Strict", 
        //         cookie.getName(), cookie.getValue(), cookie.getMaxAge()));
        
        response.addCookie(cookie);
    }
}
