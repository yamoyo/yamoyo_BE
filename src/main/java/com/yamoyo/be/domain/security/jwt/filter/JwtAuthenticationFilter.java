package com.yamoyo.be.domain.security.jwt.filter;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.JwtTokenProvider;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 *
 * Role:
 * - HTTP 요청마다 실행되어 JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정
 * - Authorization 헤더에서 Bearer 토큰을 추출하여 검증
 *
 * Complexity/Rationale:
 * - OncePerRequestFilter를 상속받아 요청당 한 번만 실행 보장
 * - SecurityFilterChain에 등록하여 OAuth2 로그인과 JWT 인증을 모두 지원
 * - JWT 토큰이 있으면 JWT 인증, 없으면 OAuth2 인증 또는 비인증 상태
 *
 * 플로우:
 * 1. Authorization 헤더에서 Bearer 토큰 추출
 * 2. 토큰 유효성 검증 (서명, 만료 등)
 * 3. 토큰에서 사용자 정보(Claims) 추출
 * 4. JwtAuthenticationToken 생성
 * 5. SecurityContext에 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청마다 실행되는 필터 메서드
     *
     * Role:
     * - Authorization 헤더에서 JWT 토큰 추출 및 검증
     * - 유효한 토큰이면 SecurityContext에 인증 정보 설정
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 다음 필터로 요청을 전달하기 위한 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Authorization 헤더에서 Bearer 토큰 추출
            String token = resolveToken(request);

            // 2. 토큰이 존재하고 유효한 경우 SecurityContext에 인증 정보 설정
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 3. 토큰에서 사용자 정보(Claims) 추출
                JwtTokenClaims claims = jwtTokenProvider.parseClaims(token);

                // 4. JwtAuthenticationToken 생성 (인증 완료 상태)
                JwtAuthenticationToken authentication = JwtAuthenticationToken.authenticated(claims);

                // 5. SecurityContext에 설정
                // - 이후 @AuthenticationPrincipal로 JwtTokenClaims 주입 가능
                // - hasRole(), @PreAuthorize 등에서 권한 검사 가능
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 인증 성공: userId={}, email={}", claims.userId(), claims.email());
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
            // 인증 실패 시 SecurityContext를 비워서 비인증 상태로 처리
            SecurityContextHolder.clearContext();
        }

        // 6. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     *
     * Role:
     * - HTTP 요청 헤더에서 "Authorization: Bearer {token}" 형식의 토큰 추출
     *
     * Complexity/Rationale:
     * - Bearer 토큰 형식이 아니면 null 반환
     * - 토큰이 없으면 OAuth2 로그인 또는 비인증 상태로 처리
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 또는 null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // "Authorization: Bearer {token}" 형식인지 확인
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // "Bearer " 이후의 토큰 문자열 추출
            return bearerToken.substring(7);
        }

        return null;
    }
}
