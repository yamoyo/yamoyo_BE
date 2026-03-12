package com.yamoyo.be.domain.collabtool.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 협업툴 투표 여부 응답")
public record MyToolVoteResponse(

        @Schema(description = "투표 완료 여부", example = "true")
        boolean voted
) {}
