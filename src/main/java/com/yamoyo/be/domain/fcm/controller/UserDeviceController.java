package com.yamoyo.be.domain.fcm.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.fcm.dto.request.DeviceUpdateRequest;
import com.yamoyo.be.domain.fcm.service.UserDeviceService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/devices")
@Slf4j
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @PostMapping
    public ApiResponse<Void> updateDevice(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @RequestBody DeviceUpdateRequest request
    ) {
        log.info("POST /api/devices - 기기 정보 업데이트, UserId: {}, DeviceType: {}", claims.userId(), request.deviceType());
        userDeviceService.updateDeviceStatus(
                claims.userId(),
                request.fcmToken(),
                request.deviceType(),
                request.deviceName()
        );

        return ApiResponse.success();
    }
}
