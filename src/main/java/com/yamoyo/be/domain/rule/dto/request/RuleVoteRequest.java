package com.yamoyo.be.domain.rule.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "규칙 투표 요청")
public record RuleVoteRequest(
        @Schema(description = "투표 대상 규칙 ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long ruleId,

        @Schema(description = "동의 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "동의 여부는 필수입니다")
        Boolean agreement       // 0: 비동의, 1: 동의
) {}
