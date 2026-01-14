package com.yamoyo.be.oauth.domain.security.controller;

import com.yamoyo.be.oauth.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.oauth.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.oauth.domain.security.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /**
     * Access Token 재발급
     *
     * Role:
     * - Access Token 만료 시 Refresh Token으로 새로운 토큰 발급
     * - Refresh Token도 함께 갱신 (Refresh Token Rotation)
     *
     * Complexity/Rationale:
     * 1. 요청 흐름:
     *    - 클라이언트가 Cookie에 Refresh Token을 포함하여 POST 요청
     *    - AuthService.refresh()로 Refresh Token 검증 및 새 토큰 발급
     *    - 새로운 Access Token + Refresh Token 반환
     *
     * 2. 프론트엔드 연동:
     *    - Access Token 만료 시 이 API 호출
     *    - 새로운 토큰을 받아서 localStorage에 저장
     *    - 이후 API 요청 시 새로운 Access Token 사용
     *
     * 3. 보안:
     *    - Refresh Token은 DB에 저장된 값과 비교 검증
     *    - 만료된 Refresh Token은 사용 불가
     *    - Refresh Token Rotation으로 탈취 방지
     *
     * Request Body:
     * (Empty) - Refresh Token is in Cookie
     *
     * Response:
     * {
     *   "grantType": "Bearer",
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "accessTokenExpiration": 600000
     * }
     *
     * @param refreshToken 쿠키에서 추출한 Refresh Token
     * @param response HTTP 응답 (새 쿠키 설정을 위해 필요)
     * @return AccessTokenResponse 새로운 Access Token 정보 (Refresh Token 제외)
     */
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        log.info("POST /api/auth/refresh - Access Token 재발급 요청");

        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh Token이 쿠키에 존재하지 않습니다.");
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
        return ResponseEntity.ok(new AccessTokenResponse(
                tokens.grantType(),
                tokens.accessToken(),
                tokens.accessTokenExpiration()
        ));
    }

    /**
     * 로그아웃
     *
     * Role:
     * - Refresh Token을 DB에서 삭제하여 무효화
     * - 이후 해당 Refresh Token으로는 Access Token 재발급 불가
     *
     * Complexity/Rationale:
     * 1. 인증 정보 추출:
     *    - @AuthenticationPrincipal JwtTokenClaims: JWT 필터에서 설정한 인증 정보
     *    - JwtAuthenticationFilter가 SecurityContext에 JwtAuthenticationToken 설정
     *    - Spring Security가 자동으로 principal(JwtTokenClaims) 주입
     *
     * 2. 로그아웃 처리:
     *    - AuthService.logout()로 DB에서 Refresh Token 삭제
     *    - Access Token은 stateless이므로 서버에서 무효화 불가 (만료까지 유효)
     *
     * 3. 프론트엔드 연동:
     *    - 로그아웃 성공 시 localStorage에서 토큰 삭제
     *    - 로그인 페이지로 리다이렉트
     *
     * 4. 보안:
     *    - Authorization 헤더에 Access Token이 있어야 호출 가능
     *    - JwtAuthenticationFilter에서 토큰 검증 후 SecurityContext 설정
     *    - 유효한 Access Token이 없으면 401 Unauthorized
     *
     * Request Headers:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * Response:
     * {
     *   "message": "로그아웃 성공"
     * }
     *
     * @param claims JWT 토큰에서 추출한 사용자 정보 (JwtAuthenticationFilter에서 설정)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal JwtTokenClaims claims) {
        log.info("POST /api/auth/logout - 로그아웃 요청, UserId: {}", claims.userId());

        authService.logout(claims.userId());

        return ResponseEntity.ok(new LogoutResponse("로그아웃 성공"));
    }

    /**
     * 로그아웃 응답 DTO
     *
     * Role:
     * - POST /api/auth/logout 응답의 Response Body
     *
     * @param message 성공 메시지
     */
    public record LogoutResponse(
            String message
    ) {}

    /**
     * Access Token 응답 DTO
     *
     * Role:
     * - Refresh Token을 제외한 Access Token 정보만 반환
     */
    public record AccessTokenResponse(
            String grantType,
            String accessToken,
            Long accessTokenExpiration
    ) {}
}
