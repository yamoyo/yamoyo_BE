package com.yamoyo.be.domain.leadergame.controller;

import com.yamoyo.be.domain.leadergame.dto.message.GameMessage;
import com.yamoyo.be.domain.leadergame.dto.message.JoinPayload;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LeaderGameWebSocketController {

    private final LeaderGameService leaderGameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * 게임 방 입장
     * /pub/room/{roomId}/join
     *
     * 응답: /user/queue/join-response 로 JOIN_SUCCESS 전송
     */
    @MessageMapping("/room/{roomId}/join")
    public void join(@DestinationVariable Long roomId, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("User {} joining room {}", userId, roomId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
            JoinPayload payload = leaderGameService.handleJoin(roomId, user);

            // 본인에게만 현재 게임 상태 전송
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/join-response",
                    GameMessage.of("JOIN_SUCCESS", payload)
            );
        } catch (Exception e) {
            log.error("Join failed", e);
            sendError(roomId, userId, e.getMessage());
        }
    }

    /**
     * 팀장 지원하기
     * /pub/room/{roomId}/volunteer
     */
    @MessageMapping("/room/{roomId}/volunteer")
    public void volunteer(@DestinationVariable Long roomId, Principal principal) {
        Long userId = extractUserId(principal);
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
    public void pass(@DestinationVariable Long roomId, Principal principal) {
        Long userId = extractUserId(principal);
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
            Principal principal,
            Map<String, Object> payload
    ) {
        Long userId = extractUserId(principal);
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
            Principal principal,
            Map<String, Object> payload
    ) {
        Long userId = extractUserId(principal);
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
     * 타이밍 게임 시작 (방장 전용)
     * /pub/room/{roomId}/start-timing
     */
    @MessageMapping("/room/{roomId}/start-timing")
    public void startTimingGame(@DestinationVariable Long roomId, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("User {} starting timing game in room {}", userId, roomId);

        try {
            leaderGameService.startTimingGame(roomId, userId);
        } catch (Exception e) {
            log.error("Start timing game failed", e);
            sendError(roomId, userId, e.getMessage());
        }
    }

    /**
     * 방 퇴장
     * /pub/room/{roomId}/leave
     */
    @MessageMapping("/room/{roomId}/leave")
    public void leave(@DestinationVariable Long roomId, Principal principal) {
        Long userId = extractUserId(principal);
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
    public void confirmResult(@DestinationVariable Long roomId, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("User {} confirmed result in room {}", userId, roomId);

        try {
            leaderGameService.confirmResult(roomId, userId);
        } catch (Exception e) {
            log.error("Confirm result failed", e);
        }
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object p = auth.getPrincipal();
            if (p instanceof JwtTokenClaims claims) {
                return claims.userId();
            }
        }
        throw new IllegalStateException("Cannot extract userId from principal");
    }

    private void sendError(Long roomId, Long userId, String message) {
        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/user/" + userId,
                GameMessage.of("ERROR", Map.of("message", message))
        );
    }
}
