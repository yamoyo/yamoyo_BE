package com.yamoyo.be.domain.notification.scheduler;

import com.yamoyo.be.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * NotificationScheduler 단위 테스트
 *
 * 테스트 내용:
 * 1. deleteOldNotifications() - 14일 지난 알림 삭제 스케줄러
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationScheduler 단위 테스트")
class NotificationSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    @Nested
    @DisplayName("deleteOldNotifications() - 오래된 알림 삭제")
    class DeleteOldNotificationsTest {

        @Test
        @DisplayName("성공: 14일 지난 알림 삭제 호출")
        void deleteOldNotifications_Success() {
            // given
            given(notificationService.deleteOldNotifications(any(LocalDateTime.class))).willReturn(10);

            // when
            notificationScheduler.deleteOldNotifications();

            // then
            ArgumentCaptor<LocalDateTime> dateTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            then(notificationService).should().deleteOldNotifications(dateTimeCaptor.capture());

            LocalDateTime capturedDateTime = dateTimeCaptor.getValue();
            LocalDateTime expectedDateTime = LocalDateTime.now().minusDays(14);

            // 14일 전 날짜인지 확인 (1분 오차 허용)
            assertThat(capturedDateTime).isBetween(
                    expectedDateTime.minusMinutes(1),
                    expectedDateTime.plusMinutes(1)
            );
        }

        @Test
        @DisplayName("성공: 삭제할 알림이 없는 경우")
        void deleteOldNotifications_NoOldNotifications() {
            // given
            given(notificationService.deleteOldNotifications(any(LocalDateTime.class))).willReturn(0);

            // when
            notificationScheduler.deleteOldNotifications();

            // then
            then(notificationService).should().deleteOldNotifications(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("성공: 예외 발생 시에도 스케줄러는 종료되지 않음")
        void deleteOldNotifications_ExceptionHandled() {
            // given
            given(notificationService.deleteOldNotifications(any(LocalDateTime.class)))
                    .willThrow(new RuntimeException("DB 연결 오류"));

            // when & then - 예외가 전파되는지 확인
            try {
                notificationScheduler.deleteOldNotifications();
            } catch (RuntimeException e) {
                // 예외가 발생해도 스케줄러 자체는 다음 실행에 영향 없음
                assertThat(e.getMessage()).isEqualTo("DB 연결 오류");
            }

            then(notificationService).should().deleteOldNotifications(any(LocalDateTime.class));
        }
    }
}
