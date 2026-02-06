package com.yamoyo.be.domain.leadergame.controller;

import com.yamoyo.be.domain.leadergame.dto.message.GameMessage;
import com.yamoyo.be.domain.leadergame.dto.message.ReloadPayload;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LeaderGameWebSocketController {

    private final LeaderGameService leaderGameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * 새로고침/재접속 시 현재 방 상태 조회
     * /pub/room/{roomId}/reload
     *
     * 응답: /sub/room/{roomId}/user/{userId} 로 RELOAD_SUCCESS 전송
     */
    @MessageMapping("/room/{roomId}/reload")
    public void reload(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        log.info("User {} reloading room {}", userId, roomId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
            ReloadPayload payload = leaderGameService.handleReload(roomId, user);

            // 본인에게만 현재 게임 상태 전송 (user-specific topic 사용)
            messagingTemplate.convertAndSend(
                    "/sub/room/" + roomId + "/user/" + userId,
                    GameMessage.of("RELOAD_SUCCESS", payload)
            );
        } catch (Exception e) {
            log.error("Reload failed", e);
            sendError(roomId, userId, e.getMessage());
        }
    }

    /**
     * 팀장 지원하기
     * /pub/room/{roomId}/volunteer
     */
    @MessageMapping("/room/{roomId}/volunteer")
    public void volunteer(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        log.info("User {} volunteering in room {}", userId, roomId);

        try {
            leaderGameService.vote(roomId, userId, true);
        } catch (Exception e) {
            log.error("Volunteer failed", e);
            sendError(roomId, userId, e.getMessage());
        }
    }

    /**
     * 팀장 지원 안하기
     * /pub/room/{roomId}/pass
     */
    @MessageMapping("/room/{roomId}/pass")
    public void pass(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        log.info("User {} passing in room {}", userId, roomId);

        try {
            leaderGameService.vote(roomId, userId, false);
        } catch (Exception e) {
            log.error("Pass failed", e);
            sendError(roomId, userId, e.getMessage());
        }
    }

    /**
     * 타이밍 게임 결과 제출
     * /pub/room/{roomId}/timing-result
     *
     * 요청 본문: { "timeDifference": 1.234 }
     * - timeDifference: |7.777 - 실제시간| 차이값 (프론트에서 계산)
     */
    @MessageMapping("/room/{roomId}/timing-result")
    public void submitTimingResult(
            @DestinationVariable Long roomId,
            SimpMessageHeaderAccessor headerAccessor,
            Map<String, Object> payload
    ) {
        Long userId = extractUserId(headerAccessor);
        Double timeDifference = ((Number) payload.get("timeDifference")).doubleValue();
        log.info("User {} submitted timing result in room {}: {}", userId, roomId, timeDifference);

        try {
            leaderGameService.submitTimingResult(roomId, userId, timeDifference);
        } catch (Exception e) {
            log.error("Submit timing result failed", e);
            sendError(roomId, userId, e.getMessage());
        }
    }

    /**
     * 게임 선택 (방장 전용)
     * /pub/room/{roomId}/select-game
     *
     * 요청 본문: { "gameType": "LADDER" | "ROULETTE" | "TIMING" }
     */
    @MessageMapping("/room/{roomId}/select-game")
    public void selectGame(
            @DestinationVariable Long roomId,
            SimpMessageHeaderAccessor headerAccessor,
            Map<String, Object> payload
    ) {
        Long userId = extractUserId(headerAccessor);
        GameType gameType = GameType.valueOf((String) payload.get("gameType"));
        log.info("User {} selected game {} in room {}", userId, gameType, roomId);

        try {
            leaderGameService.selectGame(roomId, userId, gameType);
        } catch (Exception e) {
            log.error("Select game failed", e);
            sendError(roomId, userId, e.getMessage());
        }
    }

    /**
     * 방 퇴장
     * /pub/room/{roomId}/leave
     */
    @MessageMapping("/room/{roomId}/leave")
    public void leave(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        log.info("User {} leaving room {}", userId, roomId);

        try {
            leaderGameService.handleLeave(roomId, userId);
        } catch (Exception e) {
            log.error("Leave failed", e);
        }
    }

    /**
     * 결과 확인 완료
     * /pub/room/{roomId}/confirm-result
     */
    @MessageMapping("/room/{roomId}/confirm-result")
    public void confirmResult(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractUserId(headerAccessor);
        log.info("User {} confirmed result in room {}", userId, roomId);

        try {
            leaderGameService.confirmResult(roomId, userId);
        } catch (Exception e) {
            log.error("Confirm result failed", e);
        }
    }

    private Long extractUserId(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Long userId = (Long) sessionAttributes.get("userId");
            if (userId != null) {
                return userId;
            }
        }
        throw new IllegalStateException("Cannot extract userId from sessionAttributes");
    }

    private void sendError(Long roomId, Long userId, String message) {
        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/user/" + userId,
                GameMessage.of("ERROR", Map.of("message", message))
        );
    }
}
