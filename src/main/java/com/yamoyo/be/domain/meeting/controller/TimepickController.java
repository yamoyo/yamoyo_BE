package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.service.TimepickService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}/timepick")
@RequiredArgsConstructor
public class TimepickController {

    private final TimepickService timepickService;

    @GetMapping
    public ApiResponse<TimepickResponse> getTimepick(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        log.info("타임픽 조회 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);

        TimepickResponse response = timepickService.getTimepick(teamRoomId, userId);

        return ApiResponse.success(response);
    }
}
