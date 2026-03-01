package com.yamoyo.be.domain.security.jwt;

import com.yamoyo.be.domain.user.entity.OnboardingStatus;

public record JwtTokenClaims(
        Long userId,
        String email,
        String provider,
        OnboardingStatus onboardingStatus
) {
}
