package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.request.MeetingCreateRequest;
import com.yamoyo.be.domain.meeting.dto.request.MeetingUpdateRequest;
import com.yamoyo.be.domain.meeting.dto.response.MeetingCreateResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingDeleteResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingDetailResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingListResponse;
import com.yamoyo.be.domain.meeting.dto.response.MeetingUpdateResponse;
import com.yamoyo.be.domain.meeting.entity.enums.UpdateScope;
import com.yamoyo.be.domain.meeting.service.MeetingService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/api/team-rooms/{teamRoomId}/meetings")
    public ApiResponse<MeetingCreateResponse> createMeeting(
            @PathVariable Long teamRoomId,
            @Valid @RequestBody MeetingCreateRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingCreateResponse response = meetingService.createMeeting(teamRoomId, request, userId);
        return ApiResponse.success(response);
    }

    @GetMapping("/api/team-rooms/{teamRoomId}/meetings")
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

    @GetMapping("/api/meetings/{meetingId}")
    public ApiResponse<MeetingDetailResponse> getMeetingDetail(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, userId);
        return ApiResponse.success(response);
    }

    @PutMapping("/api/meetings/{meetingId}")
    public ApiResponse<MeetingUpdateResponse> updateMeeting(
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "SINGLE") UpdateScope scope,
            @Valid @RequestBody MeetingUpdateRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingUpdateResponse response = meetingService.updateMeeting(meetingId, scope, request, userId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/api/meetings/{meetingId}")
    public ApiResponse<MeetingDeleteResponse> deleteMeeting(
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "SINGLE") UpdateScope scope,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingDeleteResponse response = meetingService.deleteMeeting(meetingId, scope, userId);
        return ApiResponse.success(response);
    }
}
