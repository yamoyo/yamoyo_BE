package com.yamoyo.be.domain.teamroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "팀 멤버 상세 응답")
public record TeamMemberDetailResponse(
        @Schema(description = "팀원 ID", example = "1")
        Long memberId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "이메일", example = "hong@example.com")
        String email,

        @Schema(description = "전공", example = "컴퓨터과학")
        String major,

        @Schema(description = "MBTI", example = "INTJ", nullable = true)
        String mbti,

        @Schema(description = "가입일자", example = "2026-01-15T10:30:00")
        LocalDateTime joinedAt
) {}
