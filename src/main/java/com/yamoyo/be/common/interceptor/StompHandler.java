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

        // 연결 요청 시점에만 토큰 검증
        if(StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = accessor.getFirstNativeHeader("Authorization");

            // 토큰 검증 로직
            if (jwt != null && jwt.startsWith("Bearer ")) {
                jwt = jwt.substring(7);
            }

            if (jwtTokenProvider.validateToken(jwt)) {
                // 토큰이 유효하면 Authentication 객체 생성
                JwtTokenClaims claims = jwtTokenProvider.parseClaims(jwt);
                Authentication authentication = JwtAuthenticationToken.authenticated(claims);

                accessor.setUser(authentication);
            } else {
                throw new YamoyoException(ErrorCode.INVALID_ACCESSTOKEN);
            }
        } else if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscription(accessor);
        }

        return message;
    }

    /**
     * SUBSCRIBE: 해당 방의 멤버인지 DB 검증 & Session에 roomId 저장(Disconnect 때 사용)
     * Session은 accessor를 말하고 서버에서 알아서 관리해줌
     */
    private void validateSubscription(StompHeaderAccessor accessor) {
        // 예 /sub/room/1
        String destination = accessor.getDestination();

        // 유저 정보 가져오기 (CONNECT 때 저쟁해둔 것)
        Authentication authentication = (Authentication) accessor.getUser();
        if(authentication == null) {
            throw new YamoyoException(ErrorCode.UNAUTHORIZED);
        }

        // Principal에서 userId 꺼내기 (구조에 맞게 캐스팅)
        JwtTokenClaims claims = (JwtTokenClaims) authentication.getPrincipal();
        Long userId = claims.userId();

        // /user/queue/* 구독은 인증된 사용자면 허용 (convertAndSendToUser용)
        if (destination != null && destination.startsWith("/user/queue/")) {
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

                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if(sessionAttributes != null) {
                    sessionAttributes.put("roomId", roomId);
                    sessionAttributes.put("userId", userId);
                }
                log.info("채팅방 구독 승인. userId: {}, roomId: {}", userId, roomId);

            } catch (NumberFormatException e) {
                log.error("잘못된 구독 주소 형식: {}", destination);
                throw new YamoyoException(ErrorCode.BAD_REQUEST);
            }
        }

    }

}
