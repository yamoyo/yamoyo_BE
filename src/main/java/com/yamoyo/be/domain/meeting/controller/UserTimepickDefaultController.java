package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.service.UserTimepickDefaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users/me/timepick-default")
@RequiredArgsConstructor
public class UserTimepickDefaultController {

    private final UserTimepickDefaultService userTimepickDefaultService;

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
