package com.yamoyo.be.domain.rule.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "팀 규칙 생성/수정 요청")
public record TeamRuleRequest(
        @Schema(description = "규칙 내용", example = "지각 시 벌금 3000원", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "규칙 내용은 필수입니다")
        String content
) {}