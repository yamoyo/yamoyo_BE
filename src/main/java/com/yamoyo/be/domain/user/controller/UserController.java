package com.yamoyo.be.domain.user.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.user.dto.response.UserResponse;
import com.yamoyo.be.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 *
 * Role:
 * - 사용자 프로필 관련 REST API 엔드포인트 제공
 * - 내 정보 조회, 프로필 수정
 *
 * Complexity/Rationale:
 * 1. REST API 설계:
 *    - GET /api/users/me: 내 프로필 조회
 *    - PUT /api/users/me: 프로필 수정 (Query Parameter로 변경할 필드 전달)
 *
 * 2. 인증 정보 주입:
 *    - @AuthenticationPrincipal JwtTokenClaims: JWT 필터에서 설정한 인증 정보
 *    - SecurityContext에 저장된 JwtAuthenticationToken에서 principal 추출
 *
 * 3. Query Parameter 방식:
 *    - 각 필드별 수정 버튼이 따로 있는 UI에 적합
 *    - null인 필드는 변경하지 않음
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(
            @AuthenticationPrincipal JwtTokenClaims claims) {
        log.info("GET /api/users/me - 내 프로필 조회 요청, UserId: {}", claims.userId());

        UserResponse response = userService.getMyProfile(claims.userId());

        return ApiResponse.success(response);
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String mbti,
            @RequestParam(required = false) Long profileImageId) {
        log.info("PUT /api/users/me - 프로필 수정 요청, UserId: {}", claims.userId());

        UserResponse response = userService.updateProfile(
                claims.userId(), name, major, mbti, profileImageId);

        return ApiResponse.success(response);
    }
}
