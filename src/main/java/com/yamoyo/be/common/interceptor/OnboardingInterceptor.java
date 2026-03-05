package com.yamoyo.be.common.interceptor;

import com.yamoyo.be.domain.user.entity.OnboardingStatus;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Onboarding Interceptor
 *
 * Role:
 * - 온보딩 완료 전 사용자에 대한 API 접근 제어
 *
 * Flow:
 * 1. SecurityContext에서 JwtAuthenticationToken 추출
 * 2. JwtTokenClaims에서 onboardingStatus 확인
 * 3. 온보딩 미완료 시 예외 발생 (403)
 *
 * 적용 대상:
 * - /api/** (온보딩 완료 전에는 핵심 API 접근 차단)
 *
 * 주의:
 * - 온보딩 상태는 JWT에 저장되어 있어 DB 조회 없이 확인 가능
 * - 온보딩 완료 시 새 토큰이 발급되어 상태가 갱신됨
 */
@Slf4j
@Component
public class OnboardingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 인증 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("인증되지 않은 요청 - URI: {}", request.getRequestURI());
            // 온보딩 인터셉터는 인증 실패를 직접 처리하지 않고 통과시킨다.
            // 최종 401 응답 여부는 Security 설정(anyRequest().authenticated())이 결정한다.
            return true;
        }

        // 2. JwtAuthenticationToken에서 claims 추출
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            log.debug("JwtAuthenticationToken이 아닌 Authentication - URI: {}", request.getRequestURI());
            return true;
        }

        JwtTokenClaims claims = jwtAuth.getJwtClaims();
        OnboardingStatus status = claims.onboardingStatus();

        // 3. JWT에서 온보딩 상태 확인 (DB 조회 없음)
        if (status == OnboardingStatus.TERMS_PENDING) {
            log.warn("온보딩(약관) 미완료 사용자 접근 차단 - UserId: {}, URI: {}",
                    claims.userId(), request.getRequestURI());
            throw new YamoyoException(
                    ErrorCode.ONBOARDING_REQUIRED,
                    Map.of("onboardingStatus", OnboardingStatus.TERMS_PENDING.name())
            );
        }

        if (status == OnboardingStatus.PROFILE_PENDING) {
            log.warn("온보딩(프로필) 미완료 사용자 접근 차단 - UserId: {}, URI: {}",
                    claims.userId(), request.getRequestURI());
            throw new YamoyoException(
                    ErrorCode.ONBOARDING_REQUIRED,
                    Map.of("onboardingStatus", OnboardingStatus.PROFILE_PENDING.name())
            );
        }

        return true; // 요청 허용
    }
}
