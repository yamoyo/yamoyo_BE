package com.yamoyo.be.domain.notification.repository;

import com.yamoyo.be.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 알림 목록 조회 (최신순, TeamRoom fetch join)
     */
    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.teamRoom
            WHERE n.user.id = :userId
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 사용자의 읽지 않은 알림 모두 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 특정 날짜 이전의 알림 삭제
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :dateTime")
    int deleteByCreatedAtBefore(@Param("dateTime") LocalDateTime dateTime);
}
