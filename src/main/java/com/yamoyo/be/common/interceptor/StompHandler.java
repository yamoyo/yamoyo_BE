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
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
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

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = accessor.getFirstNativeHeader("Authorization");
            if (jwt != null && jwt.startsWith("Bearer ")) jwt = jwt.substring(7);

            if (jwt == null || !jwtTokenProvider.validateToken(jwt)) {
                throw new YamoyoException(ErrorCode.INVALID_ACCESSTOKEN);
            }

            JwtTokenClaims claims = jwtTokenProvider.parseClaims(jwt);
            Authentication authentication = JwtAuthenticationToken.authenticated(claims);

            // 헤더 user 세팅
            accessor.setUser(authentication);

            // 2) 세션에 claims/userId 저장
            var sessionAttrs = SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders());
            if (sessionAttrs != null) {
                sessionAttrs.put("claims", claims);
                sessionAttrs.put("userId", claims.userId());
            }

            // CONNECT 프레임은 수정된 헤더로 rebuild
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscription(accessor, message); // message도 넘겨서 sessionAttributes 안전하게 접근
            return message;
        }

        return message;
    }

    /**
     * SUBSCRIBE: 해당 방의 멤버인지 DB 검증 & Session에 roomId 저장(Disconnect 때 사용)
     * Session은 accessor를 말하고 서버에서 알아서 관리해줌
     */
    private void validateSubscription(StompHeaderAccessor accessor, Message<?> message) {
        String destination = accessor.getDestination();

        Authentication authentication = (Authentication) accessor.getUser();

        // 세션에 저장해둔 claims로 복구
        JwtTokenClaims claims = null;

        if (authentication != null && authentication.getPrincipal() instanceof JwtTokenClaims jc) {
            claims = jc;
        } else {
            var sessionAttrs = SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders());
            if (sessionAttrs != null && sessionAttrs.get("claims") instanceof JwtTokenClaims jc) {
                claims = jc;
            }
        }

        if (claims == null) {
            throw new YamoyoException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = claims.userId();

        // /user/queue/* 는 인증된 사용자면 허용
        if (destination != null && destination.startsWith("/user/queue/")) {
            log.info("사용자 큐 구독 승인. userId: {}, destination: {}", userId, destination);
            return;
        }

        if (destination != null && destination.startsWith("/sub/room/")) {
            String roomIdStr = destination.replace("/sub/room/", "");
            Long roomId = Long.parseLong(roomIdStr);

            boolean isMember = teamMemberRepository.existsByTeamRoomIdAndUserId(roomId, userId);
            if (!isMember) {
                throw new YamoyoException(ErrorCode.TEAMROOM_JOIN_FORBIDDEN);
            }

            // Disconnect 등에서 쓰려고 세션에도 저장
            var sessionAttrs = SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders());
            if (sessionAttrs != null) {
                sessionAttrs.put("roomId", roomId);
                sessionAttrs.put("userId", userId);
            }

            log.info("채팅방 구독 승인. userId: {}, roomId: {}", userId, roomId);
        }
    }

}
