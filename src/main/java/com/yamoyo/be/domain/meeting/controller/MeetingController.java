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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Meeting", description = "회의 관리 API")
@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(summary = "회의 생성", description = "팀룸에 새로운 회의를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/api/team-rooms/{teamRoomId}/meetings")
    public ApiResponse<MeetingCreateResponse> createMeeting(
            @Parameter(description = "팀룸 ID") @PathVariable Long teamRoomId,
            @Valid @RequestBody MeetingCreateRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingCreateResponse response = meetingService.createMeeting(teamRoomId, request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "회의 목록 조회", description = "팀룸의 회의 목록을 조회합니다. 연도와 월로 필터링할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/api/team-rooms/{teamRoomId}/meetings")
    public ApiResponse<MeetingListResponse> getMeetingList(
            @Parameter(description = "팀룸 ID") @PathVariable Long teamRoomId,
            @Parameter(description = "조회 연도") @RequestParam(required = false) Integer year,
            @Parameter(description = "조회 월") @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingListResponse response = meetingService.getMeetingList(teamRoomId, userId, year, month);
        return ApiResponse.success(response);
    }

    @Operation(summary = "회의 상세 조회", description = "특정 회의의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회의를 찾을 수 없음")
    })
    @GetMapping("/api/meetings/{meetingId}")
    public ApiResponse<MeetingDetailResponse> getMeetingDetail(
            @Parameter(description = "회의 ID") @PathVariable Long meetingId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "회의 수정", description = "회의 정보를 수정합니다. 반복 회의의 경우 수정 범위를 지정할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회의를 찾을 수 없음")
    })
    @PutMapping("/api/meetings/{meetingId}")
    public ApiResponse<MeetingUpdateResponse> updateMeeting(
            @Parameter(description = "회의 ID") @PathVariable Long meetingId,
            @Parameter(description = "수정 범위 (SINGLE: 단일, ALL: 전체)") @RequestParam(defaultValue = "SINGLE") UpdateScope scope,
            @Valid @RequestBody MeetingUpdateRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingUpdateResponse response = meetingService.updateMeeting(meetingId, scope, request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "회의 삭제", description = "회의를 삭제합니다. 반복 회의의 경우 삭제 범위를 지정할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회의를 찾을 수 없음")
    })
    @DeleteMapping("/api/meetings/{meetingId}")
    public ApiResponse<MeetingDeleteResponse> deleteMeeting(
            @Parameter(description = "회의 ID") @PathVariable Long meetingId,
            @Parameter(description = "삭제 범위 (SINGLE: 단일, ALL: 전체)") @RequestParam(defaultValue = "SINGLE") UpdateScope scope,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        MeetingDeleteResponse response = meetingService.deleteMeeting(meetingId, scope, userId);
        return ApiResponse.success(response);
    }
}
