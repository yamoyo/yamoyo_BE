package com.yamoyo.be.domain.meeting.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.service.UserTimepickDefaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        log.info("가용시간 기본값 조회 요청 - UserId: {}", userId);

        AvailabilityResponse response = userTimepickDefaultService.getAvailability(userId);

        return ApiResponse.success(response);
    }
}
