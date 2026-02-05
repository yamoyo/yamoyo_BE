package com.yamoyo.be.event.listener;

import com.yamoyo.be.domain.leadergame.service.GameStateRedisService;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
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
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final GameStateRedisService gameStateRedisService;
    private final LeaderGameService leaderGameService;

    private static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    private static final String ONLINE = "ONLINE";
    private static final String OFFLINE = "OFFLINE";

    @EventListener
    public void handleConnectEvent(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String principalName = Optional.ofNullable(accessor.getUser())
                .map(java.security.Principal::getName)
                .orElse(null);

        log.info("WebSocket CONNECT: sessionId={}, principalName={}", sessionId, principalName);
    }

    @EventListener
    public void handleConnectedEvent(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String principalName = Optional.ofNullable(accessor.getUser())
                .map(java.security.Principal::getName)
                .orElse(null);

        log.info("WebSocket CONNECTED: sessionId={}, principalName={}", sessionId, principalName);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String destination = headerAccessor.getDestination();

        // 팀룸 구독인 경우에만 처리
        if(destination == null || !destination.startsWith("/sub/room/")) {
            return;
        }

        // roomId: destination에서 직접 파싱 (같은 SUBSCRIBE 메시지 처리 중이라 sessionAttributes 전파가 안 될 수 있음)
        Long roomId = parseRoomId(destination);
        // userId: CONNECT 시점에 이미 저장되어 있으므로 안전
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Long userId = sessionAttributes != null ? (Long) sessionAttributes.get("userId") : null;

        if(roomId == null || userId == null) {
            log.warn("roomId 또는 userId를 가져올 수 없습니다. destination: {}", destination);
            return;
        }

        // Disconnect 때 사용하기 위해 세션에 roomId 저장
        if(sessionAttributes != null) {
            sessionAttributes.put("roomId", roomId);
        }

        log.info("User {} subscribed to room {}", userId, roomId);

        // Redis에 온라인 상태 저장
        gameStateRedisService.addConnection(roomId, userId);

        // 같은 방 사람들에게 이 사람 온라인인지 브로드캐스트
        User user = userRepository.findById(userId).orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
        Long profileImageId = user.getProfileImageId();

        UserStatusResponse userStatusResponse =
                new UserStatusResponse(USER_STATUS_CHANGE, userId, user.getName(), profileImageId, ONLINE);
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
            gameStateRedisService.removeConnection(roomId, userId);

            // 게임 상태에서 접속자 제거 (비정상 종료 시에도 게임 상태 정리)
            try {
                leaderGameService.handleLeave(roomId, userId);
            } catch (Exception e) {
                log.warn("Failed to handle leave for user {} in room {}: {}", userId, roomId, e.getMessage());
            }

            // 같은 방 사람들에게 이 사람 오프라인인지 브로드캐스트
            UserStatusResponse userStatusResponse = new UserStatusResponse(USER_STATUS_CHANGE, userId, null, null, OFFLINE);
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
