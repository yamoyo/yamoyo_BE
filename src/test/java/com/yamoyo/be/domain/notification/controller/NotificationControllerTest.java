package com.yamoyo.be.domain.notification.controller;

import com.yamoyo.be.domain.notification.dto.response.NotificationResponse;
import com.yamoyo.be.domain.notification.entity.NotificationType;
import com.yamoyo.be.domain.notification.service.NotificationService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.event.listener.NotificationEventListener;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NotificationController 통합 테스트
 *
 * 테스트 내용:
 * 1. GET /api/notifications - 내 알림 목록 조회
 * 2. PATCH /api/notifications/{id}/read - 단일 알림 읽음 처리
 * 3. PATCH /api/notifications/read-all - 모든 알림 읽음 처리
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("NotificationController 통합 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationEventListener notificationEventListener;

    @MockitoBean
    private UserAgreementRepository userAgreementRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_NAME = "테스트";
    private static final String PROVIDER = "google";
    private static final Long NOTIFICATION_ID = 1L;
    private static final String BASE_URL = "/api/notifications";

    @BeforeEach
    void allowOnboardedUserAccess() {
        // OnboardingInterceptor가 /api/** 에 적용되어 있어,
        // 컨트롤러 테스트에서도 "온보딩 완료" 상태를 기본으로 맞춰준다.
        given(userAgreementRepository.hasAgreedToAllMandatoryTerms(USER_ID)).willReturn(true);

        User onboardedUser = User.create(USER_EMAIL, USER_NAME);
        onboardedUser.updateMajor("컴퓨터공학");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(onboardedUser));
    }

    @Nested
    @DisplayName("GET /api/notifications - 내 알림 목록 조회")
    class GetMyNotificationsTest {

        @Test
        @DisplayName("성공: 알림 목록 조회")
        void getMyNotifications_Success() throws Exception {
            // given
            List<NotificationResponse> responses = List.of(
                    createNotificationResponse(1L, NotificationType.TEAM_JOIN, false),
                    createNotificationResponse(2L, NotificationType.MEETING_REMIND, true)
            );
            given(notificationService.getMyNotifications(USER_ID)).willReturn(responses);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].notificationId").value(1))
                    .andExpect(jsonPath("$.data[0].type").value("TEAM_JOIN"))
                    .andExpect(jsonPath("$.data[0].isRead").value(false))
                    .andExpect(jsonPath("$.data[1].notificationId").value(2))
                    .andExpect(jsonPath("$.data[1].type").value("MEETING_REMIND"))
                    .andExpect(jsonPath("$.data[1].isRead").value(true));

            verify(notificationService).getMyNotifications(USER_ID);
        }

        @Test
        @DisplayName("성공: 알림이 없는 경우 빈 배열 반환")
        void getMyNotifications_Empty() throws Exception {
            // given
            given(notificationService.getMyNotifications(USER_ID)).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("실패: 인증되지 않은 요청")
        void getMyNotifications_Unauthorized() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(anonymous()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/{id}/read - 단일 알림 읽음 처리")
    class MarkAsReadTest {

        @Test
        @DisplayName("성공: 알림 읽음 처리")
        void markAsRead_Success() throws Exception {
            // given
            NotificationResponse response = createNotificationResponse(NOTIFICATION_ID, NotificationType.TEAM_JOIN, true);
            given(notificationService.markAsRead(NOTIFICATION_ID, USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(patch(BASE_URL + "/" + NOTIFICATION_ID + "/read")
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.notificationId").value(NOTIFICATION_ID))
                    .andExpect(jsonPath("$.data.isRead").value(true))
                    .andExpect(jsonPath("$.data.teamRoomId").value(1))
                    .andExpect(jsonPath("$.data.targetId").value(100))
                    .andExpect(jsonPath("$.data.type").value("TEAM_JOIN"));

            verify(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알림")
        void markAsRead_NotFound() throws Exception {
            // given
            given(notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                    .willThrow(new YamoyoException(ErrorCode.NOTIFICATION_NOT_FOUND));

            // when & then
            mockMvc.perform(patch(BASE_URL + "/" + NOTIFICATION_ID + "/read")
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 다른 사용자의 알림 접근")
        void markAsRead_AccessDenied() throws Exception {
            // given
            given(notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                    .willThrow(new YamoyoException(ErrorCode.NOTIFICATION_ACCESS_DENIED));

            // when & then
            mockMvc.perform(patch(BASE_URL + "/" + NOTIFICATION_ID + "/read")
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패: 인증되지 않은 요청")
        void markAsRead_Unauthorized() throws Exception {
            // when & then
            mockMvc.perform(patch(BASE_URL + "/" + NOTIFICATION_ID + "/read")
                            .with(anonymous()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/read-all - 모든 알림 읽음 처리")
    class MarkAllAsReadTest {

        @Test
        @DisplayName("성공: 모든 알림 읽음 처리")
        void markAllAsRead_Success() throws Exception {
            // given
            given(notificationService.markAllAsRead(USER_ID)).willReturn(5);

            // when & then
            mockMvc.perform(patch(BASE_URL + "/read-all")
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(5));

            verify(notificationService).markAllAsRead(USER_ID);
        }

        @Test
        @DisplayName("성공: 읽지 않은 알림이 없는 경우 0 반환")
        void markAllAsRead_NoUnread() throws Exception {
            // given
            given(notificationService.markAllAsRead(USER_ID)).willReturn(0);

            // when & then
            mockMvc.perform(patch(BASE_URL + "/read-all")
                            .with(authentication(createJwtAuthenticationToken())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(0));
        }

        @Test
        @DisplayName("실패: 인증되지 않은 요청")
        void markAllAsRead_Unauthorized() throws Exception {
            // when & then
            mockMvc.perform(patch(BASE_URL + "/read-all")
                            .with(anonymous()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== Helper Methods ==========

    private JwtAuthenticationToken createJwtAuthenticationToken() {
        JwtTokenClaims claims = new JwtTokenClaims(USER_ID, USER_EMAIL, PROVIDER);
        return JwtAuthenticationToken.authenticated(claims);
    }

    private NotificationResponse createNotificationResponse(Long notificationId, NotificationType type, boolean isRead) {
        return new NotificationResponse(
                notificationId,
                1L,  // teamRoomId
                100L,  // targetId
                type,
                type.generateTitle("테스트팀"),
                type.generateDescription("테스트팀"),
                isRead,
                LocalDateTime.now()
        );
    }
}
