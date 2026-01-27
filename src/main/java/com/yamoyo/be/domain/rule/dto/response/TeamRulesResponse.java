package com.yamoyo.be.domain.rule.dto.response;

import java.util.List;

public record TeamRulesResponse(
        List<TeamRuleInfo> teamRules
) {
    public record TeamRuleInfo(
            Long teamRuleId,
            String content
    ){}
}
