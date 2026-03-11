package com.yamoyo.be.domain.rule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 규칙 투표 내역 응답")
public record MyRuleVoteResponse(

        @Schema(description = "투표 완료 여부 (전체 규칙 투표 완료 시 true)", example = "true")
        boolean voted,

        @Schema(description = "규칙별 내 투표 내역 (미투표 규칙은 포함되지 않음)")
        List<RuleVoteDetail> votes
) {
    @Schema(description = "규칙별 투표 내역")
    public record RuleVoteDetail(

            @Schema(description = "규칙 템플릿 ID", example = "1")
            Long ruleTemplateId,

            @Schema(description = "동의 여부 (true: 동의, false: 비동의)", example = "true")
            boolean agreement
    ) {}
}
