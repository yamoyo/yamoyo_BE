package com.yamoyo.be.event.listener;

import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.leadergame.service.UserStatusService;
import com.yamoyo.be.domain.user.dto.response.UserStatusResponse;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final UserStatusService userStatusService;
    private final LeaderGameService leaderGameService;

    private static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    private static final String ONLINE = "ONLINE";
    private static final String OFFLINE = "OFFLINE";

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String destination = headerAccessor.getDestination();

        // 팀룸 구독인 경우에만 처리
        if(destination == null || !destination.startsWith("/sub/room")) {
            return;
        }

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Long roomId = sessionAttributes != null ? (Long) sessionAttributes.get("roomId") : null;
        Long userId = sessionAttributes != null ? (Long) sessionAttributes.get("userId") : null;

        if(roomId == null || userId == null) {
            log.warn("sessionAttributes에 roomId 또는 userId가 없습니다. destination: {}", destination);
            return;
        }

        log.info("User {} subscribed to room {}", userId, roomId);

        // Redis에 온라인 상태 저장
        userStatusService.saveUserOnline(roomId, userId);

        // 같은 방 사람들에게 이 사람 온라인인지 브로드캐스트
        User user = userRepository.findById(userId).orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
        Long profileImageId = user.getProfileImageId();

        UserStatusResponse userStatusResponse =
                new UserStatusResponse(USER_STATUS_CHANGE, userId, profileImageId, ONLINE);
        messagingTemplate.convertAndSend("/sub/room/" + roomId, userStatusResponse);
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long roomId = (Long) accessor.getSessionAttributes().get("roomId");
        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        if(roomId != null && userId != null) {
            log.info("User {} disconnected from room {}", userId, roomId);

            // Redis에서 온라인 상태 제거
            userStatusService.removeUserOffline(roomId, userId);

            // 게임 상태에서 접속자 제거 (비정상 종료 시에도 게임 상태 정리)
            try {
                leaderGameService.handleLeave(roomId, userId);
            } catch (Exception e) {
                log.warn("Failed to handle leave for user {} in room {}: {}", userId, roomId, e.getMessage());
            }

            // 같은 방 사람들에게 이 사람 오프라인인지 브로드캐스트
            UserStatusResponse userStatusResponse = new UserStatusResponse(USER_STATUS_CHANGE, userId, null, OFFLINE);
            messagingTemplate.convertAndSend("/sub/room/" + roomId, userStatusResponse);
        }
    }

    public Long parseRoomId(String destination) {
        try {
            return Long.parseLong(destination.replace("/sub/room/", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
