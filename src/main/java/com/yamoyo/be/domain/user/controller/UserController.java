package com.yamoyo.be.domain.user.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.user.dto.response.UserResponse;
import com.yamoyo.be.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User", description = "사용자 프로필 API")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(
            @AuthenticationPrincipal JwtTokenClaims claims) {
        log.info("GET /api/users/me - 내 프로필 조회 요청, UserId: {}", claims.userId());

        UserResponse response = userService.getMyProfile(claims.userId());

        return ApiResponse.success(response);
    }

    @Operation(summary = "프로필 수정", description = "사용자 프로필을 수정합니다. 변경할 필드만 Query Parameter로 전달하면 해당 필드만 수정됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Parameter(description = "변경할 이름") @RequestParam(required = false) String name,
            @Parameter(description = "변경할 전공") @RequestParam(required = false) String major,
            @Parameter(description = "변경할 MBTI") @RequestParam(required = false) String mbti,
            @Parameter(description = "변경할 프로필 이미지 ID") @RequestParam(required = false) Long profileImageId) {
        log.info("PUT /api/users/me - 프로필 수정 요청, UserId: {}", claims.userId());

        UserResponse response = userService.updateProfile(
                claims.userId(), name, major, mbti, profileImageId);

        return ApiResponse.success(response);
    }
}
