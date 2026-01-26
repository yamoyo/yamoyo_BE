package com.yamoyo.be.domain.user.controller;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.user.dto.response.UserResponse;
import com.yamoyo.be.domain.user.service.UserService;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 통합 테스트
 *
 * 테스트 내용:
 * 1. GET /api/users/me - 내 프로필 조회 테스트
 * 2. PUT /api/users/me - 프로필 수정 테스트
 * 3. 인증/에러 응답 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_NAME = "테스트";
    private static final String PROVIDER = "google";
    private static final String ME_ENDPOINT = "/api/users/me";

    @Nested
    @DisplayName("GET /api/users/me - 내 프로필 조회")
    class GetMyProfileTest {

        @Test
        @DisplayName("정상적으로 내 프로필 조회 성공")
        void getMyProfile_Success() throws Exception {
            // given
            UserResponse response = createUserResponse();
            given(userService.getMyProfile(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(USER_ID))
                    .andExpect(jsonPath("$.data.email").value(USER_EMAIL))
                    .andExpect(jsonPath("$.data.name").value(USER_NAME));

            verify(userService).getMyProfile(USER_ID);
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 에러")
        void getMyProfile_Unauthorized() throws Exception {
            // when & then
            mockMvc.perform(get(ME_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 404 에러")
        void getMyProfile_UserNotFound() throws Exception {
            // given
            given(userService.getMyProfile(USER_ID))
                    .willThrow(new YamoyoException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(get(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me - 프로필 수정")
    class UpdateProfileTest {

        @Test
        @DisplayName("이름만 수정 성공")
        void updateProfile_OnlyName_Success() throws Exception {
            // given
            String newName = "새이름";
            UserResponse response = new UserResponse(
                    USER_ID, USER_EMAIL, newName, "컴퓨터공학", "INTJ", 1L, LocalDateTime.now()
            );
            given(userService.updateProfile(eq(USER_ID), eq(newName), any(), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken()))
                            .param("name", newName))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value(newName));

            verify(userService).updateProfile(eq(USER_ID), eq(newName), any(), any(), any());
        }

        @Test
        @DisplayName("전공만 수정 성공")
        void updateProfile_OnlyMajor_Success() throws Exception {
            // given
            String newMajor = "컴퓨터공학";
            UserResponse response = new UserResponse(
                    USER_ID, USER_EMAIL, USER_NAME, newMajor, "INTJ", 1L, LocalDateTime.now()
            );
            given(userService.updateProfile(eq(USER_ID), any(), eq(newMajor), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken()))
                            .param("major", newMajor))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.major").value(newMajor));

            verify(userService).updateProfile(eq(USER_ID), any(), eq(newMajor), any(), any());
        }

        @Test
        @DisplayName("MBTI만 수정 성공")
        void updateProfile_OnlyMbti_Success() throws Exception {
            // given
            String newMbti = "ENFP";
            UserResponse response = new UserResponse(
                    USER_ID, USER_EMAIL, USER_NAME, "컴퓨터공학", newMbti, 1L, LocalDateTime.now()
            );
            given(userService.updateProfile(eq(USER_ID), any(), any(), eq(newMbti), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken()))
                            .param("mbti", newMbti))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.mbti").value(newMbti));

            verify(userService).updateProfile(eq(USER_ID), any(), any(), eq(newMbti), any());
        }

        @Test
        @DisplayName("프로필 이미지만 수정 성공")
        void updateProfile_OnlyProfileImage_Success() throws Exception {
            // given
            Long newProfileImageId = 2L;
            UserResponse response = new UserResponse(
                    USER_ID, USER_EMAIL, USER_NAME, "컴퓨터공학", "INTJ", newProfileImageId, LocalDateTime.now()
            );
            given(userService.updateProfile(eq(USER_ID), any(), any(), any(), eq(newProfileImageId)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken()))
                            .param("profileImageId", String.valueOf(newProfileImageId)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.profileImageId").value(newProfileImageId));

            verify(userService).updateProfile(eq(USER_ID), any(), any(), any(), eq(newProfileImageId));
        }

        @Test
        @DisplayName("모든 필드 수정 성공")
        void updateProfile_AllFields_Success() throws Exception {
            // given
            String newName = "새이름";
            String newMajor = "컴퓨터공학";
            String newMbti = "ENFP";
            Long newProfileImageId = 2L;
            UserResponse response = new UserResponse(
                    USER_ID, USER_EMAIL, newName, newMajor, newMbti, newProfileImageId, LocalDateTime.now()
            );
            given(userService.updateProfile(USER_ID, newName, newMajor, newMbti, newProfileImageId))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken()))
                            .param("name", newName)
                            .param("major", newMajor)
                            .param("mbti", newMbti)
                            .param("profileImageId", String.valueOf(newProfileImageId)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value(newName))
                    .andExpect(jsonPath("$.data.major").value(newMajor))
                    .andExpect(jsonPath("$.data.mbti").value(newMbti))
                    .andExpect(jsonPath("$.data.profileImageId").value(newProfileImageId));

            verify(userService).updateProfile(USER_ID, newName, newMajor, newMbti, newProfileImageId);
        }

        @Test
        @DisplayName("파라미터 없이 수정 요청 시 성공 (변경 없음)")
        void updateProfile_NoParams_Success() throws Exception {
            // given
            UserResponse response = createUserResponse();
            given(userService.updateProfile(eq(USER_ID), any(), any(), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).updateProfile(eq(USER_ID), any(), any(), any(), any());
        }

        @Test
        @DisplayName("인증되지 않은 요청은 401 에러")
        void updateProfile_Unauthorized() throws Exception {
            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .param("name", "새이름"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 프로필 수정 시 404 에러")
        void updateProfile_UserNotFound() throws Exception {
            // given
            given(userService.updateProfile(eq(USER_ID), any(), any(), any(), any()))
                    .willThrow(new YamoyoException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(put(ME_ENDPOINT)
                            .with(authentication(createJwtAuthenticationToken()))
                            .param("name", "새이름"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ========== Helper Methods ==========

    private JwtAuthenticationToken createJwtAuthenticationToken() {
        JwtTokenClaims claims = new JwtTokenClaims(USER_ID, USER_EMAIL, PROVIDER);
        return JwtAuthenticationToken.authenticated(claims);
    }

    private UserResponse createUserResponse() {
        return new UserResponse(
                USER_ID,
                USER_EMAIL,
                USER_NAME,
                "컴퓨터공학",
                "INTJ",
                1L,
                LocalDateTime.now()
        );
    }
}
