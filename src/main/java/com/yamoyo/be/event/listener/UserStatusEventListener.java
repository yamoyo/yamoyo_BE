package com.yamoyo.be.event.listener;

import com.yamoyo.be.domain.leadergame.service.GameStateRedisService;
import com.yamoyo.be.domain.user.dto.response.UserStatusResponse;
import com.yamoyo.be.event.event.MemberRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserStatusEventListener {

    private final GameStateRedisService gameStateRedisService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleMemberRemoved(MemberRemovedEvent event) {
        // Redis 에서 상태처리
        gameStateRedisService.removeConnection(event.roomId(), event.userId());

        // event.type = LEAVE 또는 KICK
        UserStatusResponse response = new UserStatusResponse(
                event.type(),
                event.userId(),
                null,
                null,
                null
        );
        messagingTemplate.convertAndSend("/sub/room/" + event.roomId(), response);
    }
}
