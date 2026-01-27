package com.yamoyo.be.domain.rule.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TeamRuleRequest(
        @NotBlank(message = "규칙 내용은 필수입니다")
        String content
) {
}
