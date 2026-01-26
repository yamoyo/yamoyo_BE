package com.yamoyo.be.domain.user.dto.response;

import com.yamoyo.be.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String name,
        String major,
        String mbti,
        Long profileImageId,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getMajor(),
                user.getMbti(),
                user.getProfileImageId(),
                user.getCreatedAt()
        );
    }
}