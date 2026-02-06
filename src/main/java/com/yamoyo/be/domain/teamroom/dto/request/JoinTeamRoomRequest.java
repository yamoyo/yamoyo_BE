package com.yamoyo.be.domain.teamroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "팀룸 입장 요청")
public record JoinTeamRoomRequest (
        @Schema(description = "초대 토큰", example = "abc123def456")
        @NotBlank(message = "초대링크는 필수입니다")
        String inviteToken
){}
