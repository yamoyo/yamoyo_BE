package com.yamoyo.be.domain.user.entity;

/**
 * OnboardingStatus Enum
 *
 * Role:
 * - 사용자의 온보딩 진행 상태를 나타내는 열거형
 * - OAuth2 로그인 후 리다이렉트 경로 결정에 사용
 */
public enum OnboardingStatus {
    /**
     * 약관 동의 필요
     * - 필수 약관에 동의하지 않은 상태
     * - 리다이렉트: /onboarding/terms
     */
    TERMS_PENDING,

    /**
     * 프로필 작성 필요
     * - 약관은 동의했으나 프로필 정보가 미완성
     * - 리다이렉트: /onboarding/profile
     */
    PROFILE_PENDING,

    /**
     * 온보딩 완료
     * - 모든 필수 절차 완료
     * - 리다이렉트: /home
     */
    COMPLETED
}
