package com.yamoyo.be.domain.teamroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 링크 응답")
public record InviteLinkResponse(
        @Schema(description = "초대 토큰", example = "abc123def456")
        String token,

        @Schema(description = "토큰 만료까지 남은 시간 (초)", example = "86400")
        long expiresInSeconds
) {}
