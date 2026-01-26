package com.yamoyo.be.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
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
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    private OnboardingInterceptor interceptor;
    private ObjectMapper objectMapper;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        interceptor = new OnboardingInterceptor(userAgreementRepository, objectMapper);
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

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

        String responseBody = response.getContentAsString();
        assertThat(responseBody).contains("\"code\":403");
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
    @DisplayName("OAuth2User가 아닌 Principal - 통과")
    void preHandle_NotOAuth2User_PassThrough() throws Exception {
        // given
        Authentication authentication = mock(Authentication.class);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn("anonymousUser");

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

    private void setupSecurityContext(Long userId) {
        OAuth2User oAuth2User = mock(OAuth2User.class);
        given(oAuth2User.getAttributes()).willReturn(Map.of("userId", userId));

        Authentication authentication = mock(Authentication.class);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(oAuth2User);

        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void setupSecurityContextWithNullUserId() {
        OAuth2User oAuth2User = mock(OAuth2User.class);
        given(oAuth2User.getAttributes()).willReturn(Map.of()); // userId 없음

        Authentication authentication = mock(Authentication.class);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(oAuth2User);

        SecurityContext securityContext = mock(SecurityContext.class);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
