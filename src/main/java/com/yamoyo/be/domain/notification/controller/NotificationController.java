package com.yamoyo.be.domain.notification.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.notification.dto.response.NotificationResponse;
import com.yamoyo.be.domain.notification.service.NotificationService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification", description = "알림 API")
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal JwtTokenClaims claims) {
        log.info("GET /api/notifications - 알림 목록 조회, UserId: {}", claims.userId());

        List<NotificationResponse> notifications = notificationService.getMyNotifications(claims.userId());
        return ApiResponse.success(notifications);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다. 읽음 처리된 알림 정보를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Parameter(description = "알림 ID") @PathVariable Long notificationId) {
        log.info("PATCH /api/notifications/{}/read - 알림 읽음 처리, UserId: {}", notificationId, claims.userId());

        NotificationResponse response = notificationService.markAsRead(notificationId, claims.userId());
        return ApiResponse.success(response);
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "현재 로그인한 사용자의 모든 알림을 읽음 처리합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모두 읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/read-all")
    public ApiResponse<Integer> markAllAsRead(
            @AuthenticationPrincipal JwtTokenClaims claims) {
        log.info("PATCH /api/notifications/read-all - 모든 알림 읽음 처리, UserId: {}", claims.userId());

        int updatedCount = notificationService.markAllAsRead(claims.userId());
        return ApiResponse.success(updatedCount);
    }
}
