package com.yamoyo.be.domain.user.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.user.dto.request.ProfileSetupRequest;
import com.yamoyo.be.domain.user.dto.request.TermsAgreementRequest;
import com.yamoyo.be.domain.user.service.OnBoardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User Controller
 *
 * Role:
 * - 사용자 관련 API 엔드포인트 제공
 * - 온보딩 과정의 약관 동의, 프로필 설정 API 포함
 *
 * Endpoints:
 * - POST /api/users/terms : 약관 동의
 * - POST /api/users/profile : 프로필 설정
 */
@Tag(name = "Onboarding", description = "온보딩 API - 약관 동의 및 프로필 설정")
@Slf4j
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnBoardingController {

    private final OnBoardingService onBoardingService;

    @Operation(summary = "약관 동의", description = "서비스 이용약관 및 개인정보 처리방침에 동의합니다. 필수 약관에 모두 동의해야 온보딩을 진행할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "약관 동의 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 약관 미동의 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약관을 찾을 수 없음")
    })
    @PostMapping("/terms")
    public ApiResponse<Void> agreeToTerms(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody TermsAgreementRequest request
    ) {
        log.info("약관 동의 요청 - UserId: {}, Agreements: {}", claims.userId(), request.agreements().size());

        onBoardingService.agreeToTerms(claims.userId(), request);

        return ApiResponse.success();
    }

    @Operation(summary = "프로필 설정", description = "사용자 프로필을 설정합니다. 이름, 전공, MBTI, 프로필 이미지를 입력합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/profile")
    public ApiResponse<Void> setupProfile(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody ProfileSetupRequest request
    ) {
        log.info("프로필 설정 요청 - UserId: {}, Name: {}, Major: {}, MBTI: {}",
                claims.userId(), request.name(), request.major(), request.mbti());

        onBoardingService.setupProfile(claims.userId(), request);

        return ApiResponse.success();
    }
}
