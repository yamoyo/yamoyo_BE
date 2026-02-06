package com.yamoyo.be.domain.teamroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀룸 생성 응답")
public record CreateTeamRoomResponse(
        @Schema(description = "생성된 팀룸 ID", example = "1")
        Long teamRoomId,

        @Schema(description = "초대 토큰", example = "abc123def456")
        String inviteToken,

        @Schema(description = "토큰 만료까지 남은 시간 (초)", example = "86400")
        Long expiresInSeconds
) {}
