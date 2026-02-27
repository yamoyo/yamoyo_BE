package com.yamoyo.be.domain.collabtool.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "협업툴 제안 요청")
public record ProposeToolRequest(
        @Schema(description = "협업툴 카테고리 ID", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "카테고리 ID는 필수입니다")
        Integer categoryId,

        @Schema(description = "제안할 툴 ID", example = "[1, 3]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "툴 ID는 필수입니다")
        List<Integer> toolIds
) {}