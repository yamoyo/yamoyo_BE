package com.yamoyo.be.domain.notification.entity;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoom;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(length = 100)
    private String title;

    @Column(length = 255)
    private String message;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isRead;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Notification create(User user, TeamRoom teamRoom, Long targetId, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.user = user;
        notification.teamRoom = teamRoom;
        notification.targetId = targetId;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.isRead = false;
        return notification;
    }

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
