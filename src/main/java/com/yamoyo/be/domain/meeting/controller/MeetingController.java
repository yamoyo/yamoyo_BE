package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.request.MeetingCreateRequest;
import com.yamoyo.be.domain.meeting.dto.response.MeetingCreateResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingListResponse;
import com.yamoyo.be.domain.meeting.service.MeetingService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ApiResponse<MeetingCreateResponse> createMeeting(
            @PathVariable Long teamRoomId,
            @Valid @RequestBody MeetingCreateRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingCreateResponse response = meetingService.createMeeting(teamRoomId, request, userId);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<MeetingListResponse> getMeetingList(
            @PathVariable Long teamRoomId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingListResponse response = meetingService.getMeetingList(teamRoomId, userId, year, month);
        return ApiResponse.success(response);
    }
}
