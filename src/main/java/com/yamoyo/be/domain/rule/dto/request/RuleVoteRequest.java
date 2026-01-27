package com.yamoyo.be.domain.rule.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RuleVoteRequest(
        @NotNull Long ruleId,

        @NotNull(message = "동의 여부는 필수입니다")
        @Min(value = 0, message = "동의 여부는 0 또는 1이어야 합니다")
        @Max(value = 1, message = "동의 여부는 0 또는 1이어야 합니다")
        Integer agreement       // 0: 비동의, 1: 동의
) {
    public Boolean toBoolean() {
        return agreement == 1;
    }
}
