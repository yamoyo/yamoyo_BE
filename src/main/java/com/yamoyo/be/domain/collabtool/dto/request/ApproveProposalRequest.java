package com.yamoyo.be.domain.collabtool.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "협업툴 제안 승인/반려 요청")
public record ApproveProposalRequest(
        @Schema(description = "승인 여부 (true: 승인, false: 반려)", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "승인 여부는 필수입니다")
        Boolean isApproved  // true: 승인, false: 반려
) {}