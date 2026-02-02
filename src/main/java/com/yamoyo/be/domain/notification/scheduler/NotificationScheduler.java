package com.yamoyo.be.domain.notification.scheduler;

import com.yamoyo.be.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final int NOTIFICATION_RETENTION_DAYS = 14;

    private final NotificationService notificationService;

    /**
     * 매일 새벽 1시에 14일 지난 알림 삭제
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void deleteOldNotifications() {
        log.info("오래된 알림 삭제 스케줄러 시작");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(NOTIFICATION_RETENTION_DAYS);
        int deletedCount = notificationService.deleteOldNotifications(cutoffDate);

        log.info("오래된 알림 삭제 스케줄러 완료: {}건 삭제", deletedCount);
    }
}
