package com.yamoyo.be.common.interceptor;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.user.entity.OnboardingStatus;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * OnboardingInterceptor 단위 테스트
 *
 * 테스트 내용:
 * 1. 온보딩 완료 사용자 - 요청 허용
 * 2. 약관 미동의 사용자 - 요청 차단 (403)
 * 3. 프로필 미설정 사용자 - 요청 차단 (403)
 * 4. 인증되지 않은 요청 - 통과 (Spring Security에서 처리)
 */
@ExtendWith(MockitoExtension.class)
class OnboardingInterceptorTest {

    private OnboardingInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        interceptor = new OnboardingInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRequestURI("/api/onboarding/profile");
    }

    @Test
    @DisplayName("온보딩 완료 사용자 - 요청 허용")
    void preHandle_OnboardingCompleted_AllowRequest() throws Exception {
        // given
        setupSecurityContext(USER_ID, OnboardingStatus.COMPLETED);

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("약관 미동의 사용자 - 요청 차단 (403)")
    void preHandle_TermsNotAgreed_BlockRequest() throws Exception {
        // given
        setupSecurityContext(USER_ID, OnboardingStatus.TERMS_PENDING);

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(YamoyoException.class)
                .satisfies(ex -> {
                    YamoyoException ye = (YamoyoException) ex;
                    assertThat(ye.getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
                    assertThat(ye.getDetails()).containsEntry("onboardingStatus", OnboardingStatus.TERMS_PENDING.name());
                });
    }

    @Test
    @DisplayName("프로필 미설정 사용자 - 요청 차단 (403)")
    void preHandle_ProfileNotCompleted_BlockRequest() throws Exception {
        // given
        setupSecurityContext(USER_ID, OnboardingStatus.PROFILE_PENDING);

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(YamoyoException.class)
                .satisfies(ex -> {
                    YamoyoException ye = (YamoyoException) ex;
                    assertThat(ye.getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
                    assertThat(ye.getDetails()).containsEntry("onboardingStatus", OnboardingStatus.PROFILE_PENDING.name());
                });
    }

    @Test
    @DisplayName("인증되지 않은 요청 - 통과 (Spring Security에서 처리)")
    void preHandle_NotAuthenticated_PassThrough() throws Exception {
        // given
        SecurityContextHolder.clearContext();

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("JwtAuthenticationToken이 아닌 Authentication - 통과")
    void preHandle_NotJwtAuthenticationToken_PassThrough() throws Exception {
        // given
        Authentication authentication = mock(Authentication.class);
        given(authentication.isAuthenticated()).willReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
    }

    // ========== Helper Methods ==========

    private static final String USER_EMAIL = "test@example.com";
    private static final String PROVIDER = "google";

    private void setupSecurityContext(Long userId, OnboardingStatus status) {
        JwtTokenClaims claims = new JwtTokenClaims(userId, USER_EMAIL, PROVIDER, status);
        JwtAuthenticationToken authentication = JwtAuthenticationToken.authenticated(claims);

        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
