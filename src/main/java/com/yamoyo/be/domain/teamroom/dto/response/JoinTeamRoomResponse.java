package com.yamoyo.be.domain.teamroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀룸 입장 응답")
public record JoinTeamRoomResponse(
        @Schema(description = "입장 결과", example = "JOINED")
        JoinResult joinResult,

        @Schema(description = "팀룸 ID", example = "1")
        Long teamRoomId,

        @Schema(description = "멤버 ID", example = "1")
        Long memberId
) {
    @Schema(description = "입장 결과 타입")
    public enum JoinResult {
        @Schema(description = "신규 가입")
        JOINED,

        @Schema(description = "재입장")
        ENTERED,

        @Schema(description = "차단됨")
        BANNED
    }
}
