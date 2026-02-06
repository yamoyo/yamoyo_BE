package com.yamoyo.be.domain.leadergame.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게임 참가자 정보")
public record GameParticipant (
    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "프로필 이미지 ID", example = "1")
    String profileImageId
)
{
    public static GameParticipant of(Long userId, String name, String profileImageId) {
        return new GameParticipant(userId, name, profileImageId);
    }
}