package com.yamoyo.be.domain.security.handler;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.security.oauth.CookieProperties;
import com.yamoyo.be.domain.security.oauth.CustomOAuth2User;
import com.yamoyo.be.domain.security.refreshtoken.RefreshTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Custom Logout Success Handler
 *
 * Role:
 * - Spring Security 로그아웃 성공 시 후처리를 담당
 * - DB에서 Refresh Token 삭제
 * - Refresh Token 쿠키 삭제
 * - 메인 페이지("/")로 리다이렉트
 *
 * Complexity/Rationale:
 * 1. DB RefreshToken 삭제:
 *    - Authentication에서 userId 추출
 *    - JWT 인증: JwtAuthenticationToken → JwtTokenClaims
 *    - OAuth2 인증: OAuth2AuthenticationToken → CustomOAuth2User
 *
 * 2. 쿠키 삭제:
 *    - HttpOnly 쿠키는 JavaScript에서 삭제 불가
 *    - 서버에서 maxAge=0으로 설정하여 브라우저에게 삭제 요청
 *
 * 3. 리다이렉트:
 *    - 로그아웃 후 메인 페이지("/")로 이동
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieProperties cookieProperties;

    @Override
    @Transactional
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        log.info("로그아웃 성공 처리");

        // 1. DB에서 Refresh Token 삭제
        Long userId = extractUserId(authentication);
        if (userId != null) {
            refreshTokenRepository.deleteByUserId(userId);
            log.info("DB RefreshToken 삭제 완료 - UserId: {}", userId);
        }

        // 2. Refresh Token 쿠키 삭제
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // 3. JSESSIONID 쿠키 삭제 (세션 기반 인증 호환)
        ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        log.info("로그아웃 성공 - 쿠키 삭제 및 리다이렉트 완료");
    }

    /**
     * Authentication에서 userId 추출
     *
     * @param authentication 인증 객체 (JWT 또는 OAuth2)
     * @return userId 또는 null
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        // JWT 인증인 경우
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            JwtTokenClaims claims = jwtAuth.getJwtClaims();
            return claims != null ? claims.userId() : null;
        }

        // OAuth2 인증인 경우
        if (authentication instanceof OAuth2AuthenticationToken) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomOAuth2User customOAuth2User) {
                return customOAuth2User.getUserId();
            }
        }

        return null;
    }
}
