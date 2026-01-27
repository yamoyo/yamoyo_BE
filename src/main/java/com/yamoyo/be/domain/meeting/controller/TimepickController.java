package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.request.AvailabilitySubmitRequest;
import com.yamoyo.be.domain.meeting.dto.request.PreferredBlockSubmitRequest;
import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.service.TimepickService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        log.info("타임픽 조회 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);

        TimepickResponse response = timepickService.getTimepick(teamRoomId, userId);

        return ApiResponse.success(response);
    }

    @PostMapping("/availability")
    public ApiResponse<Void> submitAvailability(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody AvailabilitySubmitRequest request
    ) {
        Long userId = claims.userId();
        log.info("가용시간 제출 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
        timepickService.submitAvailability(teamRoomId, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/preferred-block")
    public ApiResponse<Void> submitPreferredBlock(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody PreferredBlockSubmitRequest request
    ) {
        Long userId = claims.userId();
        log.info("선호시간대 제출 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
        timepickService.submitPreferredBlock(teamRoomId, userId, request);
        return ApiResponse.success();
    }
}
