package com.yamoyo.be.domain.notification.service;

import com.yamoyo.be.domain.notification.dto.response.NotificationResponse;
import com.yamoyo.be.domain.notification.entity.Notification;
import com.yamoyo.be.domain.notification.entity.NotificationType;
import com.yamoyo.be.domain.notification.repository.NotificationRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.user.entity.User;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * NotificationService 단위 테스트
 *
 * 테스트 내용:
 * 1. getMyNotifications() - 내 알림 목록 조회
 * 2. markAsRead() - 단일 알림 읽음 처리
 * 3. markAllAsRead() - 모든 알림 읽음 처리
 * 4. deleteOldNotifications() - 오래된 알림 삭제
 * 5. saveNotification() / saveAll() - 알림 저장
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long NOTIFICATION_ID = 1L;
    private static final Long TEAM_ROOM_ID = 1L;
    private static final Long TARGET_ID = 100L;

    @Nested
    @DisplayName("getMyNotifications() - 내 알림 목록 조회")
    class GetMyNotificationsTest {

        @Test
        @DisplayName("성공: 알림 목록 조회")
        void getMyNotifications_Success() {
            // given
            Notification notification1 = createNotification(1L, NotificationType.TEAM_JOIN, false);
            Notification notification2 = createNotification(2L, NotificationType.MEETING_REMIND, true);
            given(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .willReturn(List.of(notification1, notification2));

            // when
            List<NotificationResponse> result = notificationService.getMyNotifications(USER_ID);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).notificationId()).isEqualTo(1L);
            assertThat(result.get(0).type()).isEqualTo(NotificationType.TEAM_JOIN);
            assertThat(result.get(0).isRead()).isFalse();
            assertThat(result.get(1).notificationId()).isEqualTo(2L);
            assertThat(result.get(1).type()).isEqualTo(NotificationType.MEETING_REMIND);
            assertThat(result.get(1).isRead()).isTrue();
        }

        @Test
        @DisplayName("성공: 알림이 없는 경우 빈 리스트 반환")
        void getMyNotifications_Empty() {
            // given
            given(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .willReturn(Collections.emptyList());

            // when
            List<NotificationResponse> result = notificationService.getMyNotifications(USER_ID);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("markAsRead() - 단일 알림 읽음 처리")
    class MarkAsReadTest {

        @Test
        @DisplayName("성공: 알림 읽음 처리")
        void markAsRead_Success() {
            // given
            Notification notification = createNotification(NOTIFICATION_ID, NotificationType.TEAM_JOIN, false);
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

            // when
            NotificationResponse result = notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

            // then
            assertThat(result.notificationId()).isEqualTo(NOTIFICATION_ID);
            assertThat(result.isRead()).isTrue();
            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("성공: 이미 읽은 알림도 정상 처리")
        void markAsRead_AlreadyRead_Success() {
            // given
            Notification notification = createNotification(NOTIFICATION_ID, NotificationType.TEAM_JOIN, true);
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

            // when
            NotificationResponse result = notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

            // then
            assertThat(result.isRead()).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알림")
        void markAsRead_NotificationNotFound() {
            // given
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
        }

        @Test
        @DisplayName("실패: 다른 사용자의 알림 접근")
        void markAsRead_AccessDenied() {
            // given - 예외 발생으로 NotificationResponse.from()이 호출되지 않으므로 mock 사용
            User user = mock(User.class);
            given(user.getId()).willReturn(USER_ID);

            Notification notification = mock(Notification.class);
            given(notification.getUser()).willReturn(user);
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, OTHER_USER_ID))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("markAllAsRead() - 모든 알림 읽음 처리")
    class MarkAllAsReadTest {

        @Test
        @DisplayName("성공: 여러 알림 읽음 처리")
        void markAllAsRead_Success() {
            // given
            given(notificationRepository.markAllAsReadByUserId(USER_ID)).willReturn(5);

            // when
            int result = notificationService.markAllAsRead(USER_ID);

            // then
            assertThat(result).isEqualTo(5);
            then(notificationRepository).should().markAllAsReadByUserId(USER_ID);
        }

        @Test
        @DisplayName("성공: 읽지 않은 알림이 없는 경우 0 반환")
        void markAllAsRead_NoUnreadNotifications() {
            // given
            given(notificationRepository.markAllAsReadByUserId(USER_ID)).willReturn(0);

            // when
            int result = notificationService.markAllAsRead(USER_ID);

            // then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("deleteOldNotifications() - 오래된 알림 삭제")
    class DeleteOldNotificationsTest {

        @Test
        @DisplayName("성공: 14일 지난 알림 삭제")
        void deleteOldNotifications_Success() {
            // given
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(14);
            given(notificationRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).willReturn(10);

            // when
            int result = notificationService.deleteOldNotifications(cutoffDate);

            // then
            assertThat(result).isEqualTo(10);
            then(notificationRepository).should().deleteByCreatedAtBefore(cutoffDate);
        }

        @Test
        @DisplayName("성공: 삭제할 알림이 없는 경우 0 반환")
        void deleteOldNotifications_NoOldNotifications() {
            // given
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(14);
            given(notificationRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).willReturn(0);

            // when
            int result = notificationService.deleteOldNotifications(cutoffDate);

            // then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("saveNotification() / saveAll() - 알림 저장")
    class SaveNotificationTest {

        @Test
        @DisplayName("성공: 단일 알림 저장")
        void saveNotification_Success() {
            // given
            Notification notification = mock(Notification.class);

            // when
            notificationService.saveNotification(notification);

            // then
            then(notificationRepository).should().save(notification);
        }

        @Test
        @DisplayName("성공: 여러 알림 일괄 저장")
        void saveAll_Success() {
            // given
            Notification notification1 = mock(Notification.class);
            Notification notification2 = mock(Notification.class);
            List<Notification> notifications = List.of(notification1, notification2);

            // when
            notificationService.saveAll(notifications);

            // then
            then(notificationRepository).should().saveAll(notifications);
        }
    }

    // ========== Helper Methods ==========

    private Notification createNotification(Long notificationId, NotificationType type, boolean isRead) {
        User user = User.create("test@example.com", "테스트");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        TeamRoom teamRoom = createTeamRoom();

        Notification notification = Notification.create(
                user,
                teamRoom,
                TARGET_ID,
                type,
                type.generateTitle("테스트팀"),
                type.generateDescription("테스트팀")
        );
        ReflectionTestUtils.setField(notification, "notificationId", notificationId);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());
        if (isRead) {
            notification.markAsRead();
        }
        return notification;
    }

    private TeamRoom createTeamRoom() {
        TeamRoom teamRoom = mock(TeamRoom.class);
        // NotificationResponse.from()에서 getTeamRoom().getId() 호출
        given(teamRoom.getId()).willReturn(TEAM_ROOM_ID);
        return teamRoom;
    }
}
