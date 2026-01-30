package com.yamoyo.be.domain.collabtool.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 협업툴 제안 요청
 */
public record ProposeToolRequest(
        @NotNull(message = "카테고리 ID는 필수입니다")
        Integer categoryId,

        @NotNull(message = "툴 ID는 필수입니다")
        Integer toolId
) {}