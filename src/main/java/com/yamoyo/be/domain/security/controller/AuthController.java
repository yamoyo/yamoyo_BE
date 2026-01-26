package com.yamoyo.be.domain.security.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.domain.security.service.AuthService;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Auth Controller
 *
 * Role:
 * - JWT 인증 관련 REST API 엔드포인트 제공
 * - Access Token 재발급, 로그아웃 처리
 *
 * Complexity/Rationale:
 * 1. REST API 설계:
 *    - POST /api/auth/refresh: Access Token 재발급
 *    - POST /api/auth/logout: 로그아웃 (Refresh Token 무효화)
 *
 * 2. 인증 정보 주입:
 *    - @AuthenticationPrincipal JwtTokenClaims: JWT 필터에서 설정한 인증 정보
 *    - SecurityContext에 저장된 JwtAuthenticationToken에서 principal 추출
 *    - Controller 메서드 파라미터로 자동 주입
 *
 * 3. 예외 처리:
 *    - AuthService에서 발생하는 예외는 GlobalExceptionHandler에서 처리
 *    - RefreshTokenException → 401 Unauthorized
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @PostMapping("/refresh")
    public ApiResponse<AccessTokenResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        log.info("POST /api/auth/refresh - Access Token 재발급 요청");

        if (refreshToken == null) {
            throw new YamoyoException(ErrorCode.BAD_REQUEST);
        }

        JwtTokenDto tokens = authService.refresh(refreshToken);

        // Refresh Token Rotation: 새로운 Refresh Token을 HttpOnly Cookie로 설정
        Cookie cookie = new Cookie("refresh_token", tokens.refreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTPS 적용 시 true로 변경 필요
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        response.addCookie(cookie);

        // Body에는 Access Token 정보만 포함 (Refresh Token 노출 방지)
        return ApiResponse.success(new AccessTokenResponse(
                tokens.grantType(),
                tokens.accessToken(),
                tokens.accessTokenExpiration()
        ));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal JwtTokenClaims claims,
            HttpServletResponse response) {
        log.info("DELETE /api/auth/me - 회원 탈퇴 요청, UserId: {}", claims.userId());

        authService.withdraw(claims.userId());

        // Refresh Token 쿠키 삭제
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // HTTPS 적용 시 true로 변경 필요
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 즉시 만료
        response.addCookie(cookie);

        return ApiResponse.success();
    }

    public record AccessTokenResponse(
            String grantType,
            String accessToken,
            Long accessTokenExpiration
    ) {}
}
