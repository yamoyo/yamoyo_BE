package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.service.UserTimepickDefaultService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Timepick Default", description = "사용자 타임픽 기본값 API")
@Slf4j
@RestController
@RequestMapping("/api/users/me/timepick-default")
@RequiredArgsConstructor
public class UserTimepickDefaultController {

    private final UserTimepickDefaultService userTimepickDefaultService;

    @Operation(summary = "가용시간 기본값 조회", description = "현재 로그인한 사용자의 가용시간 기본값을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/availability")
    public ApiResponse<AvailabilityResponse> getAvailability(
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        log.info("가용시간 기본값 조회 요청 - UserId: {}", userId);

        AvailabilityResponse response = userTimepickDefaultService.getAvailability(userId);

        return ApiResponse.success(response);
    }
}
