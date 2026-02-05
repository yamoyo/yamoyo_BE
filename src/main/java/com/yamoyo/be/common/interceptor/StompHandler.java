package com.yamoyo.be.common.interceptor;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.JwtTokenProvider;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // 우선수위 최상
public class StompHandler implements ChannelInterceptor {

    private final TeamMemberRepository teamMemberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("STOMP CONNECT: sessionId={}, user={}", accessor.getSessionId(), accessor.getUser());
            String jwt = accessor.getFirstNativeHeader("Authorization");
            if (jwt == null) {
                jwt = accessor.getFirstNativeHeader("authorization");
            }
            log.info("STOMP CONNECT: authorizationHeaderPresent={}", jwt != null);

            if (jwt != null && jwt.startsWith("Bearer ")) {
                jwt = jwt.substring(7);
            }
            log.info("STOMP CONNECT: bearerStrippedPresent={}", jwt != null && !jwt.isBlank());

            boolean valid = jwtTokenProvider.validateToken(jwt);
            log.info("STOMP CONNECT: jwtValid={}", valid);
            if (!valid) {
                throw new YamoyoException(ErrorCode.INVALID_ACCESSTOKEN);
            }

            JwtTokenClaims claims = jwtTokenProvider.parseClaims(jwt);
            log.info("STOMP CONNECT: claimsUserId={}", claims.userId());
            Authentication authentication = JwtAuthenticationToken.authenticated(claims);
            accessor.setUser(authentication);
            log.info("STOMP CONNECT: userSetTo={}", accessor.getUser());

            // sessionAttributes에 인증 정보 저장 (SUBSCRIBE에서 사용)
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("userId", claims.userId());
                sessionAttributes.put("claims", claims);
            }

        } else {
            // CONNECT 이후 user가 아직 전파되지 않은 메시지 대비 (레이스 컨디션 완화)
            if (accessor.getUser() == null) {
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                Object claimsObj = sessionAttributes != null ? sessionAttributes.get("claims") : null;
                if (claimsObj instanceof JwtTokenClaims claims) {
                    Authentication authentication = JwtAuthenticationToken.authenticated(claims);
                    accessor.setUser(authentication);
                    log.info("STOMP: user re-injected for command={}, sessionId={}", accessor.getCommand(), accessor.getSessionId());
                }
            }
        }

        if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            log.info("STOMP SUBSCRIBE: sessionId={}, user={}, destination={}", accessor.getSessionId(), accessor.getUser(), accessor.getDestination());
            validateSubscription(accessor);
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    /**
     * SUBSCRIBE: 해당 방의 멤버인지 DB 검증 & Session에 roomId 저장(Disconnect 때 사용)
     * Session은 accessor를 말하고 서버에서 알아서 관리해줌
     */
    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        // sessionAttributes에서 userId 가져오기 (CONNECT 때 저장해둔 것)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Long userId = sessionAttributes != null ? (Long) sessionAttributes.get("userId") : null;

        if(userId == null) {
            log.error("SUBSCRIBE 시 userId가 없습니다. destination: {}", destination);
            throw new YamoyoException(ErrorCode.UNAUTHORIZED);
        }

        // /user/queue/* 구독은 인증된 사용자면 허용 (convertAndSendToUser용)
        if (destination != null && destination.startsWith("/user/queue/")) {
            if (userId == null) {
                log.warn("인증 없는 사용자 큐 구독 시도. destination: {}", destination);
                throw new YamoyoException(ErrorCode.UNAUTHORIZED);
            }
            log.info("사용자 큐 구독 승인. userId: {}, destination: {}", userId, destination);
            return;
        }

        // 목적지가 채팅방 구독인 경우만 체크 (/sub/room/{id})
        // 만약 알림 등 다른 구독 주소도 있다면 여기서 분기 처리 필요
        if (destination != null && destination.startsWith("/sub/room/")) {

            try {
                // URL에서 roomId 파싱 (/sub/room/100 -> 100)
                String roomIdStr = destination.replace("/sub/room/", "");
                Long roomId = Long.parseLong(roomIdStr);

                // DB(또는 Redis)에서 멤버 여부 확인
                // select count(*) from team_member where team_room_id = ? and user_id = ?
                boolean isMember = teamMemberRepository.existsByTeamRoomIdAndUserId(roomId, userId);

                if (!isMember) {
                    log.warn("미가입 사용자의 채팅방 구독 시도 감지! userId: {}, roomId: {}", userId, roomId);
                    throw new YamoyoException(ErrorCode.TEAMROOM_JOIN_FORBIDDEN);
                }

                if(sessionAttributes != null) {
                    sessionAttributes.put("roomId", roomId);
                }
                log.info("채팅방 구독 승인. userId: {}, roomId: {}", userId, roomId);

            } catch (NumberFormatException e) {
                log.error("잘못된 구독 주소 형식: {}", destination);
                throw new YamoyoException(ErrorCode.BAD_REQUEST);
            }
        }

    }

}
