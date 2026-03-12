package com.yamoyo.be.domain.rule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 규칙 투표 여부 응답")
public record MyRuleVoteResponse(

        @Schema(description = "투표 완료 여부 (전체 규칙 투표 완료 시 true)", example = "true")
        boolean voted
) {}
