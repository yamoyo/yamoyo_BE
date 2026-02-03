package com.yamoyo.be.domain.security.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.domain.security.oauth.CookieProperties;
import com.yamoyo.be.domain.security.service.AuthService;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Tag(name = "Auth", description = "인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieProperties cookieProperties;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Operation(summary = "Access Token 재발급", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. Refresh Token Rotation이 적용되어 새로운 Refresh Token도 함께 발급됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Refresh Token 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
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
        cookie.setSecure(cookieProperties.secure()); // HTTPS 적용 시 true로 변경 필요
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        response.addCookie(cookie);

        // Body에는 Access Token 정보만 포함 (Refresh Token 노출 방지)
        return ApiResponse.success(new AccessTokenResponse(
                tokens.grantType(),
                tokens.accessToken(),
                tokens.accessTokenExpiration()
        ));
    }

    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 삭제합니다. 관련된 모든 데이터(RefreshToken, UserAgreement, UserDevice, SocialAccount 등)가 함께 삭제됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
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
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0); // 쿠키 즉시 만료
        response.addCookie(cookie);

        return ApiResponse.success();
    }

    @Schema(description = "Access Token 응답")
    public record AccessTokenResponse(
            @Schema(description = "인증 타입", example = "Bearer")
            String grantType,

            @Schema(description = "Access Token")
            String accessToken,

            @Schema(description = "Access Token 만료 시간 (Unix timestamp)", example = "1704067200000")
            Long accessTokenExpiration
    ) {}
}
