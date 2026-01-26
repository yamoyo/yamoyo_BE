package com.yamoyo.be.domain.user.service;

import com.yamoyo.be.domain.user.dto.response.UserResponse;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * UserService 단위 테스트
 *
 * 테스트 내용:
 * 1. getMyProfile() - 내 프로필 조회 테스트
 * 2. updateProfile() - 프로필 수정 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_NAME = "테스트";

    @Test
    @DisplayName("getMyProfile() - 정상 프로필 조회")
    void getMyProfile_Success() {
        // given
        User user = createUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.getMyProfile(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo(USER_EMAIL);
        assertThat(response.name()).isEqualTo(USER_NAME);
    }

    @Test
    @DisplayName("getMyProfile() - 존재하지 않는 사용자 조회 시 예외 발생")
    void getMyProfile_UserNotFound_ThrowsException() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyProfile(USER_ID))
                .isInstanceOf(YamoyoException.class)
                .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("updateProfile() - 이름만 수정")
    void updateProfile_OnlyName_Success() {
        // given
        User user = createUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        String newName = "새이름";

        // when
        UserResponse response = userService.updateProfile(USER_ID, newName, null, null, null);

        // then
        assertThat(response.name()).isEqualTo(newName);
    }

    @Test
    @DisplayName("updateProfile() - 전공만 수정")
    void updateProfile_OnlyMajor_Success() {
        // given
        User user = createUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        String newMajor = "컴퓨터공학";

        // when
        UserResponse response = userService.updateProfile(USER_ID, null, newMajor, null, null);

        // then
        assertThat(response.major()).isEqualTo(newMajor);
    }

    @Test
    @DisplayName("updateProfile() - MBTI만 수정")
    void updateProfile_OnlyMbti_Success() {
        // given
        User user = createUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        String newMbti = "INTJ";

        // when
        UserResponse response = userService.updateProfile(USER_ID, null, null, newMbti, null);

        // then
        assertThat(response.mbti()).isEqualTo(newMbti);
    }

    @Test
    @DisplayName("updateProfile() - 프로필 이미지만 수정")
    void updateProfile_OnlyProfileImage_Success() {
        // given
        User user = createUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        Long newProfileImageId = 2L;

        // when
        UserResponse response = userService.updateProfile(USER_ID, null, null, null, newProfileImageId);

        // then
        assertThat(response.profileImageId()).isEqualTo(newProfileImageId);
    }

    @Test
    @DisplayName("updateProfile() - 모든 필드 수정")
    void updateProfile_AllFields_Success() {
        // given
        User user = createUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        String newName = "새이름";
        String newMajor = "컴퓨터공학";
        String newMbti = "INTJ";
        Long newProfileImageId = 2L;

        // when
        UserResponse response = userService.updateProfile(USER_ID, newName, newMajor, newMbti, newProfileImageId);

        // then
        assertThat(response.name()).isEqualTo(newName);
        assertThat(response.major()).isEqualTo(newMajor);
        assertThat(response.mbti()).isEqualTo(newMbti);
        assertThat(response.profileImageId()).isEqualTo(newProfileImageId);
    }

    @Test
    @DisplayName("updateProfile() - 존재하지 않는 사용자 프로필 수정 시 예외 발생")
    void updateProfile_UserNotFound_ThrowsException() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(USER_ID, "새이름", null, null, null))
                .isInstanceOf(YamoyoException.class)
                .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    private User createUser() {
        User user = User.create(USER_EMAIL, USER_NAME);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
