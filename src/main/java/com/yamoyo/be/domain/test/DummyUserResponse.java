package com.yamoyo.be.domain.test;

public record DummyUserResponse(
        Long userId,
        String email,
        String name,
        String major,
        String mbti,
        String accessToken,
        String refreshToken
) {
}
