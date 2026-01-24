package com.yamoyo.be.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Onboarding Interceptor
 *
 * Role:
 * - 온보딩 단계별 API 접근 제어
 * - 약관 미동의 사용자가 프로필 설정 API에 접근하는 것을 차단
 *
 * Flow:
 * 1. SecurityContext에서 인증된 사용자 정보 추출
 * 2. DB에서 실시간으로 약관 동의 여부 확인
 * 3. 약관 미동의 시 403 Forbidden 응답 반환
 *
 * 적용 대상:
 * - POST /api/users/profile (프로필 설정 API)
 *
 * 주의:
 * - CustomOAuth2User의 onboardingStatus는 로그인 시점에만 계산되므로 사용 불가
 * - DB에서 실시간 조회해야 약관 동의 후 상태가 정확히 반영됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingInterceptor implements HandlerInterceptor {

    private final UserAgreementRepository userAgreementRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 인증 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("인증되지 않은 요청 - URI: {}", request.getRequestURI());
            return true; // Spring Security에서 처리하도록 넘김
        }

        // 2. OAuth2User에서 userId 추출
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oAuth2User)) {
            log.debug("OAuth2User가 아닌 Principal - URI: {}", request.getRequestURI());
            return true;
        }

        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        if (userId == null) {
            log.warn("userId가 null - URI: {}", request.getRequestURI());
            return true;
        }

        // 3. DB에서 실시간으로 약관 동의 여부 확인
        boolean hasAgreedToTerms = userAgreementRepository.hasAgreedToAllMandatoryTerms(userId);

        if (!hasAgreedToTerms) {
            log.warn("약관 미동의 사용자의 프로필 API 접근 차단 - UserId: {}, URI: {}",
                    userId, request.getRequestURI());

            // 4. 403 Forbidden 응답 반환
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> errorResponse = ApiResponse.fail(ErrorCode.TERMS_NOT_AGREED);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

            return false; // 요청 차단
        }

        return true; // 요청 허용
    }
}
