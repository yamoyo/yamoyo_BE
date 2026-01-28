package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.request.AvailabilitySubmitRequest;
import com.yamoyo.be.domain.meeting.dto.request.PreferredBlockSubmitRequest;
import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.service.TimepickService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Timepick", description = "타임픽 (일정 조율) API")
@Slf4j
@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}/timepick")
@RequiredArgsConstructor
public class TimepickController {

    private final TimepickService timepickService;

    @Operation(summary = "타임픽 조회", description = "팀룸의 타임픽 정보를 조회합니다. 참여자들의 가용시간과 선호시간대를 포함합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public ApiResponse<TimepickResponse> getTimepick(
            @Parameter(description = "팀룸 ID") @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        log.info("타임픽 조회 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);

        TimepickResponse response = timepickService.getTimepick(teamRoomId, userId);

        return ApiResponse.success(response);
    }

    @Operation(summary = "가용시간 제출", description = "사용자의 가용시간을 제출합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/availability")
    public ApiResponse<Void> submitAvailability(
            @Parameter(description = "팀룸 ID") @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody AvailabilitySubmitRequest request
    ) {
        Long userId = claims.userId();
        log.info("가용시간 제출 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
        timepickService.submitAvailability(teamRoomId, userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "선호시간대 제출", description = "사용자의 선호시간대를 제출합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/preferred-block")
    public ApiResponse<Void> submitPreferredBlock(
            @Parameter(description = "팀룸 ID") @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody PreferredBlockSubmitRequest request
    ) {
        Long userId = claims.userId();
        log.info("선호시간대 제출 요청 - TeamRoomId: {}, UserId: {}", teamRoomId, userId);
        timepickService.submitPreferredBlock(teamRoomId, userId, request);
        return ApiResponse.success();
    }
}
