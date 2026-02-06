package com.yamoyo.be.domain.user.dto.response;

import com.yamoyo.be.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 프로필 응답")
public record UserResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "전공", example = "컴퓨터공학")
        String major,

        @Schema(description = "MBTI", example = "INTJ")
        String mbti,

        @Schema(description = "프로필 이미지 ID", example = "1")
        Long profileImageId,

        @Schema(description = "가입일시", example = "2024-01-01T00:00:00")
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