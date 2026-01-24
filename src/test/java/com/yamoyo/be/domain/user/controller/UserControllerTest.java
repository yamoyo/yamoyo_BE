package com.yamoyo.be.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamoyo.be.domain.user.dto.ProfileSetupRequest;
import com.yamoyo.be.domain.user.dto.TermsAgreementRequest;
import com.yamoyo.be.domain.user.dto.TermsAgreementRequest.TermAgreement;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.domain.user.service.UserService;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 통합 테스트
 *
 * 테스트 내용:
 * 1. POST /api/users/terms - 약관 동의 API 테스트
 * 2. POST /api/users/profile - 프로필 설정 API 테스트
 * 3. Validation 테스트
 * 4. 에러 응답 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserAgreementRepository userAgreementRepository;

    private static final Long USER_ID = 1L;
    private static final String TERMS_ENDPOINT = "/api/users/terms";
    private static final String PROFILE_ENDPOINT = "/api/users/profile";

    @Nested
    @DisplayName("POST /api/users/terms - 약관 동의")
    class AgreeToTermsTest {

        @Test
        @DisplayName("모든 필수 약관에 동의하면 성공")
        void agreeToTerms_Success() throws Exception {
            // given
            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true),
                    new TermAgreement(2L, true)
            ));

            willDoNothing().given(userService).agreeToTerms(eq(USER_ID), any(TermsAgreementRequest.class));

            // when & then
            mockMvc.perform(post(TERMS_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).agreeToTerms(eq(USER_ID), any(TermsAgreementRequest.class));
        }

        @Test
        @DisplayName("필수 약관 미동의 시 400 에러")
        void agreeToTerms_MandatoryNotAgreed_BadRequest() throws Exception {
            // given
            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true),
                    new TermAgreement(2L, false)
            ));

            willThrow(new YamoyoException(ErrorCode.MANDATORY_TERMS_NOT_AGREED))
                    .given(userService).agreeToTerms(eq(USER_ID), any(TermsAgreementRequest.class));

            // when & then
            mockMvc.perform(post(TERMS_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("약관 목록이 null이면 400 에러")
        void agreeToTerms_NullAgreements_BadRequest() throws Exception {
            // given
            String requestJson = "{\"agreements\": null}";

            // when & then
            mockMvc.perform(post(TERMS_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 에러")
        void agreeToTerms_Unauthorized() throws Exception {
            // given
            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true)
            ));

            // when & then
            mockMvc.perform(post(TERMS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/users/profile - 프로필 설정")
    class SetupProfileTest {

        @Test
        @DisplayName("프로필 설정 성공")
        void setupProfile_Success() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "INTJ",
                    1L
            );

            // Interceptor에서 약관 동의 여부 확인
            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);
            willDoNothing().given(userService).setupProfile(eq(USER_ID), any(ProfileSetupRequest.class));

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).setupProfile(eq(USER_ID), any(ProfileSetupRequest.class));
        }

        @Test
        @DisplayName("약관 미동의 시 403 에러 (Interceptor)")
        void setupProfile_TermsNotAgreed_Forbidden() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "INTJ",
                    1L
            );

            // Interceptor에서 차단
            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(false);

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("이름이 10자 초과하면 400 에러")
        void setupProfile_NameTooLong_BadRequest() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "가나다라마바사아자차카", // 11자
                    "컴퓨터공학과",
                    "INTJ",
                    1L
            );

            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이름에 특수문자가 포함되면 400 에러")
        void setupProfile_NameWithSpecialChar_BadRequest() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동!@#",
                    "컴퓨터공학과",
                    "INTJ",
                    1L
            );

            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("MBTI가 4자가 아니면 400 에러")
        void setupProfile_InvalidMbti_BadRequest() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "INT", // 3자
                    1L
            );

            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("MBTI가 소문자면 400 에러")
        void setupProfile_LowercaseMbti_BadRequest() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "intj", // 소문자
                    1L
            );

            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("프로필 이미지 없이 설정 가능")
        void setupProfile_WithoutProfileImage_Success() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "ENFP",
                    1L
            );

            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);
            willDoNothing().given(userService).setupProfile(eq(USER_ID), any(ProfileSetupRequest.class));

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("영문 이름도 허용")
        void setupProfile_EnglishName_Success() throws Exception {
            // given
            ProfileSetupRequest request = new ProfileSetupRequest(
                    "JohnDoe",
                    "Computer Science",
                    "ENTP",
                    1L
            );

            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);
            willDoNothing().given(userService).setupProfile(eq(USER_ID), any(ProfileSetupRequest.class));

            // when & then
            mockMvc.perform(post(PROFILE_ENDPOINT)
                            .with(authentication(createOAuth2AuthenticationToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    // ========== Helper Methods ==========

    private OAuth2AuthenticationToken createOAuth2AuthenticationToken() {
        Map<String, Object> attributes = Map.of(
                "sub", "123456",
                "email", "test@example.com",
                "name", "테스트",
                "userId", USER_ID
        );

        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")),
                attributes,
                "sub"
        );

        return new OAuth2AuthenticationToken(
                oAuth2User,
                oAuth2User.getAuthorities(),
                "google"
        );
    }
}
