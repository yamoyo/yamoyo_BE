package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.request.AvailabilitySubmitRequest;
import com.yamoyo.be.domain.meeting.dto.request.PreferredBlockSubmitRequest;
import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.service.TimepickService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;


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

    @PostMapping("/availability")
    public ApiResponse<Void> submitAvailability(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @Valid @RequestBody AvailabilitySubmitRequest request
    ) {
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        log.info("가용시간 제출 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
        timepickService.submitAvailability(teamRoomId, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/preferred-block")
    public ApiResponse<Void> submitPreferredBlock(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @Valid @RequestBody PreferredBlockSubmitRequest request
    ) {
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        log.info("선호시간대 제출 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
        timepickService.submitPreferredBlock(teamRoomId, userId, request);
        return ApiResponse.success();
    }
}
