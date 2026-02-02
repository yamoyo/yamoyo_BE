package com.yamoyo.be.domain.notification.dto.response;

import com.yamoyo.be.domain.notification.entity.Notification;
import com.yamoyo.be.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "1")
        Long notificationId,

        @Schema(description = "팀룸 ID", example = "1")
        Long teamRoomId,

        @Schema(description = "대상 ID (규칙 ID, 회의 ID 등)", example = "1")
        Long targetId,

        @Schema(description = "알림 타입", example = "MEETING_REMIND")
        NotificationType type,

        @Schema(description = "알림 제목", example = "[프로젝트A] 회의 10분 전 리마인드")
        String title,

        @Schema(description = "알림 메시지", example = "프로젝트A 팀의 회의 시간 10분 전입니다!")
        String message,

        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "생성 일시", example = "2024-01-15T14:30:00")
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getTeamRoom().getId(),
                notification.getTargetId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
