package com.yamoyo.be.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamoyo.be.common.annotation.HostOnly;
import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.authentication.JwtAuthenticationToken;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HostCheckInterceptor implements HandlerInterceptor {

    private final TeamMemberRepository teamMemberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // HandlerMethod인지 확인
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // @HostOnly 애노테이션이 붙어있는지 확인
        HostOnly hostOnly = handlerMethod.getMethodAnnotation(HostOnly.class);
        if (hostOnly == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return true;
        }

        JwtTokenClaims claims = jwtAuth.getJwtClaims();
        Long userId = claims.userId();
        if (userId == null) {
            return true;
        }

        Long roomId = extractRoomId(request);
        if (roomId == null) {
            log.warn("roomId 추출 실패 - URI: {}", request.getRequestURI());
            return true;
        }

        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId)
                .orElse(null);

        if (member == null || !member.hasManagementAuthority()) {
            log.warn("방장 권한 없음 - UserId: {}, RoomId: {}", userId, roomId);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> errorResponse = ApiResponse.fail(ErrorCode.NOT_ROOM_HOST);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

            return false;
        }

        return true;
    }

    private Long extractRoomId(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (pathVariables == null) {
            return null;
        }

        String roomIdStr = pathVariables.get("roomId");
        if (roomIdStr == null) {
            return null;
        }

        try {
            return Long.parseLong(roomIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
