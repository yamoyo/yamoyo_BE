package com.yamoyo.be.domain.notification.service;

import com.yamoyo.be.domain.notification.dto.response.NotificationResponse;
import com.yamoyo.be.domain.notification.entity.Notification;
import com.yamoyo.be.domain.notification.repository.NotificationRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void saveNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    @Transactional
    public void saveAll(List<Notification> notifications) {
        notificationRepository.saveAll(notifications);
    }

    /**
     * 내 알림 목록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * 단일 알림 읽음 처리
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new YamoyoException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    /**
     * 특정 날짜 이전의 알림 삭제
     */
    @Transactional
    public int deleteOldNotifications(LocalDateTime before) {
        int deletedCount = notificationRepository.deleteByCreatedAtBefore(before);
        log.info("{}일 이전 알림 {}건 삭제 완료", before, deletedCount);
        return deletedCount;
    }
}
