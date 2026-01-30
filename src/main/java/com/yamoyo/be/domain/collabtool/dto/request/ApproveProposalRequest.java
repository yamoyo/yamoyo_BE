package com.yamoyo.be.domain.collabtool.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 제안 승인/반려 요청
 */
public record ApproveProposalRequest(
        @NotNull(message = "승인 여부는 필수입니다")
        Boolean isApproved  // true: 승인, false: 반려
) {}