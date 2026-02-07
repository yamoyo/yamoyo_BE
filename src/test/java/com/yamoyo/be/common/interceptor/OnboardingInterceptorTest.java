package com.yamoyo.be.common.interceptor;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.user.entity.OnboardingStatus;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
 * 1. 약관 동의 사용자 - 요청 허용
 * 2. 약관 미동의 사용자 - 요청 차단 (403)
 * 3. 인증되지 않은 요청 - 통과 (Spring Security에서 처리)
 */
@ExtendWith(MockitoExtension.class)
class OnboardingInterceptorTest {

    @Mock
    private UserAgreementRepository userAgreementRepository;

    @Mock
    private UserRepository userRepository;

    private OnboardingInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        interceptor = new OnboardingInterceptor(userAgreementRepository, userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRequestURI("/api/onboarding/profile");
    }

    @Test
    @DisplayName("약관 동의 사용자 - 요청 허용")
    void preHandle_TermsAgreed_AllowRequest() throws Exception {
        // given
        setupSecurityContext(USER_ID);
        given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);
        User onboardedUser = User.create(USER_EMAIL, "테스트");
        onboardedUser.updateMajor("컴퓨터공학");
        given(userRepository.findById(USER_ID)).willReturn(java.util.Optional.of(onboardedUser));

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
        setupSecurityContext(USER_ID);
        given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(false);

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

    @Test
    @DisplayName("userId가 null인 경우 - 통과")
    void preHandle_UserIdNull_PassThrough() throws Exception {
        // given
        setupSecurityContextWithNullUserId();

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
    }

    // ========== Helper Methods ==========

    private static final String USER_EMAIL = "test@example.com";
    private static final String PROVIDER = "google";

    private void setupSecurityContext(Long userId) {
        JwtTokenClaims claims = new JwtTokenClaims(userId, USER_EMAIL, PROVIDER);
        JwtAuthenticationToken authentication = JwtAuthenticationToken.authenticated(claims);

        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void setupSecurityContextWithNullUserId() {
        JwtTokenClaims claims = new JwtTokenClaims(null, USER_EMAIL, PROVIDER);
        JwtAuthenticationToken authentication = JwtAuthenticationToken.authenticated(claims);

        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
