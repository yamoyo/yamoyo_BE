package com.yamoyo.be.common.interceptor;

import com.yamoyo.be.domain.user.entity.OnboardingStatus;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
 * 2. JwtTokenClaims에서 userId 추출
 * 3. DB에서 약관 동의/프로필 완료 여부 확인
 * 4. 온보딩 미완료 시 예외 발생 (403)
 *
 * 적용 대상:
 * - /api/** (온보딩 완료 전에는 핵심 API 접근 차단)
 *
 * 주의:
 * - 온보딩 완료 상태는 DB에서 실시간 조회해야 정확히 반영됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingInterceptor implements HandlerInterceptor {

    private final UserAgreementRepository userAgreementRepository;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 인증 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("인증되지 않은 요청 - URI: {}", request.getRequestURI());
            return true; // Spring Security에서 처리하도록 넘김
        }

        // 2. JwtAuthenticationToken에서 userId 추출
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            log.debug("JwtAuthenticationToken이 아닌 Authentication - URI: {}", request.getRequestURI());
            return true;
        }

        JwtTokenClaims claims = jwtAuth.getJwtClaims();
        Long userId = claims.userId();
        if (userId == null) {
            log.warn("userId가 null - URI: {}", request.getRequestURI());
            return true;
        }

        // 3. DB에서 약관 동의/프로필 완료 여부 확인
        boolean hasAgreedToTerms = userAgreementRepository.hasAgreedToAllMandatoryTerms(userId);
        if (!hasAgreedToTerms) {
            log.warn("온보딩(약관) 미완료 사용자 접근 차단 - UserId: {}, URI: {}",
                    userId, request.getRequestURI());
            throw new YamoyoException(
                    ErrorCode.ONBOARDING_REQUIRED,
                    Map.of("onboardingStatus", OnboardingStatus.TERMS_PENDING.name())
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        boolean isProfileCompleted = user.getMajor() != null;
        if (!isProfileCompleted) {
            log.warn("온보딩(프로필) 미완료 사용자 접근 차단 - UserId: {}, URI: {}",
                    userId, request.getRequestURI());
            throw new YamoyoException(
                    ErrorCode.ONBOARDING_REQUIRED,
                    Map.of("onboardingStatus", OnboardingStatus.PROFILE_PENDING.name())
            );
        }

        return true; // 요청 허용
    }
}
