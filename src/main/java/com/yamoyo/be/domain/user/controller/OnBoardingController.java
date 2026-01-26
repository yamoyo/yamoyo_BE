package com.yamoyo.be.domain.user.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.user.dto.request.ProfileSetupRequest;
import com.yamoyo.be.domain.user.dto.request.TermsAgreementRequest;
import com.yamoyo.be.domain.user.service.OnBoardingService;
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
@Slf4j
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnBoardingController {

    private final OnBoardingService onBoardingService;

    @PostMapping("/terms")
    public ApiResponse<Void> agreeToTerms(
            @AuthenticationPrincipal JwtTokenClaims claims,
            @Valid @RequestBody TermsAgreementRequest request
    ) {
        log.info("약관 동의 요청 - UserId: {}, Agreements: {}", claims.userId(), request.agreements().size());

        onBoardingService.agreeToTerms(claims.userId(), request);

        return ApiResponse.success();
    }

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
