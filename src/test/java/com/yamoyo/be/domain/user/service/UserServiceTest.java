package com.yamoyo.be.domain.user.service;

import com.yamoyo.be.domain.user.dto.ProfileSetupRequest;
import com.yamoyo.be.domain.user.dto.TermsAgreementRequest;
import com.yamoyo.be.domain.user.dto.TermsAgreementRequest.TermAgreement;
import com.yamoyo.be.domain.user.entity.Term;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.entity.UserRole;
import com.yamoyo.be.domain.user.repository.TermRepository;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserService 단위 테스트
 *
 * 테스트 내용:
 * 1. agreeToTerms() - 약관 동의 처리 테스트
 * 2. setupProfile() - 프로필 설정 처리 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private UserAgreementRepository userAgreementRepository;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_NAME = "테스트";

    @Nested
    @DisplayName("agreeToTerms() - 약관 동의")
    class AgreeToTermsTest {

        @Test
        @DisplayName("모든 필수 약관에 동의하면 성공")
        void agreeToTerms_AllMandatoryAgreed_Success() {
            // given
            User user = createUser();
            Term serviceTerm = createTerm(1L, "SERVICE", true);
            Term privacyTerm = createTerm(2L, "PRIVACY", true);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(termRepository.findByIsActiveAndIsMandatory(true, true))
                    .willReturn(List.of(serviceTerm, privacyTerm));
            given(termRepository.findById(1L)).willReturn(Optional.of(serviceTerm));
            given(termRepository.findById(2L)).willReturn(Optional.of(privacyTerm));

            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true),
                    new TermAgreement(2L, true)
            ));

            // when
            userService.agreeToTerms(USER_ID, request);

            // then
            verify(userAgreementRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("필수 약관 중 하나라도 미동의하면 예외 발생")
        void agreeToTerms_MandatoryNotAgreed_ThrowsException() {
            // given
            User user = createUser();
            Term serviceTerm = createTerm(1L, "SERVICE", true);
            Term privacyTerm = createTerm(2L, "PRIVACY", true);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(termRepository.findByIsActiveAndIsMandatory(true, true))
                    .willReturn(List.of(serviceTerm, privacyTerm));

            // 서비스 약관만 동의, 개인정보 약관은 미동의
            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true),
                    new TermAgreement(2L, false)
            ));

            // when & then
            assertThatThrownBy(() -> userService.agreeToTerms(USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MANDATORY_TERMS_NOT_AGREED));

            verify(userAgreementRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("필수 약관이 요청에 누락되면 예외 발생")
        void agreeToTerms_MandatoryMissing_ThrowsException() {
            // given
            User user = createUser();
            Term serviceTerm = createTerm(1L, "SERVICE", true);
            Term privacyTerm = createTerm(2L, "PRIVACY", true);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(termRepository.findByIsActiveAndIsMandatory(true, true))
                    .willReturn(List.of(serviceTerm, privacyTerm));

            // 서비스 약관만 포함 (개인정보 약관 누락)
            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true)
            ));

            // when & then
            assertThatThrownBy(() -> userService.agreeToTerms(USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MANDATORY_TERMS_NOT_AGREED));
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외 발생")
        void agreeToTerms_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true)
            ));

            // when & then
            assertThatThrownBy(() -> userService.agreeToTerms(USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 약관 ID로 동의하면 예외 발생")
        void agreeToTerms_TermNotFound_ThrowsException() {
            // given
            User user = createUser();
            Term serviceTerm = createTerm(1L, "SERVICE", true);

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(termRepository.findByIsActiveAndIsMandatory(true, true))
                    .willReturn(List.of(serviceTerm));
            given(termRepository.findById(1L)).willReturn(Optional.of(serviceTerm));
            given(termRepository.findById(999L)).willReturn(Optional.empty());

            TermsAgreementRequest request = new TermsAgreementRequest(List.of(
                    new TermAgreement(1L, true),
                    new TermAgreement(999L, true) // 존재하지 않는 약관
            ));

            // when & then
            assertThatThrownBy(() -> userService.agreeToTerms(USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TERMS_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("setupProfile() - 프로필 설정")
    class SetupProfileTest {

        @Test
        @DisplayName("약관 동의 후 프로필 설정 성공")
        void setupProfile_AfterTermsAgreed_Success() {
            // given
            User user = createUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);

            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "INTJ",
                    1L
            );

            // when
            userService.setupProfile(USER_ID, request);

            // then
            assertThat(user.getName()).isEqualTo("홍길동");
            assertThat(user.getMajor()).isEqualTo("컴퓨터공학과");
            assertThat(user.getMbti()).isEqualTo("INTJ");
            assertThat(user.getProfileImageId()).isEqualTo(1L);
            assertThat(user.getUserRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("약관 미동의 상태에서 프로필 설정 시 예외 발생")
        void setupProfile_TermsNotAgreed_ThrowsException() {
            // given
            User user = createUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(false);

            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "INTJ",
                    1L
            );

            // when & then
            assertThatThrownBy(() -> userService.setupProfile(USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TERMS_NOT_AGREED));

            // UserRole이 변경되지 않았는지 확인
            assertThat(user.getUserRole()).isEqualTo(UserRole.GUEST);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외 발생")
        void setupProfile_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "INTJ",
                    1L
            );

            // when & then
            assertThatThrownBy(() -> userService.setupProfile(USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("프로필 이미지 없이 설정 가능")
        void setupProfile_WithoutProfileImage_Success() {
            // given
            User user = createUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);

            ProfileSetupRequest request = new ProfileSetupRequest(
                    "홍길동",
                    "컴퓨터공학과",
                    "ENFP",
                    null // 프로필 이미지 없음
            );

            // when
            userService.setupProfile(USER_ID, request);

            // then
            assertThat(user.getName()).isEqualTo("홍길동");
            assertThat(user.getProfileImageId()).isNull();
            assertThat(user.getUserRole()).isEqualTo(UserRole.USER);
        }
    }

    // ========== Helper Methods ==========

    private User createUser() {
        User user = User.create(USER_EMAIL, USER_NAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private Term createTerm(Long id, String termsType, Boolean isMandatory) {
        Term term = Term.create(termsType, termsType + " 약관", "내용", "1.0", isMandatory);
        ReflectionTestUtils.setField(term, "id", id);
        ReflectionTestUtils.setField(term, "isActive", true);
        return term;
    }
}
