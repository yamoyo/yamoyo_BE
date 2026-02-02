package com.yamoyo.be.event.event;

import com.yamoyo.be.domain.notification.entity.NotificationType;

import java.util.Map;

public record NotificationEvent(
        Long teamRoomId,
        Long targetId,
        NotificationType type,
        String teamLogoUrl,
        Map<String, String> args
) {
    public static NotificationEvent ofSingle(
            Long teamRoomId,
            Long targetId,
            NotificationType type) {
        return new NotificationEvent(teamRoomId, targetId, type, null, null);
    }
}
