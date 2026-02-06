package com.yamoyo.be.domain.rule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "확정된 팀 규칙 목록 응답")
public record TeamRulesResponse(

        @Schema(description = "팀 규칙 목록")
        List<TeamRuleInfo> teamRules
) {
    @Schema(description = "팀 규칙 정보")
    public record TeamRuleInfo(

            @Schema(description = "팀 규칙 ID", example = "1")
            Long teamRuleId,

            @Schema(description = "규칙 내용", example = "회의는 매주 월요일 오후 9시에 진행한다")
            String content
    ){}
}
