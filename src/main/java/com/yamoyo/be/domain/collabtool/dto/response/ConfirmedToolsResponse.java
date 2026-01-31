package com.yamoyo.be.domain.collabtool.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "확정된 협업툴 조회 응답")
public record ConfirmedToolsResponse(

        @Schema(description = "카테고리별 확정된 협업툴 목록")
        List<CategoryTools> confirmedTools
) {
    @Schema(description = "카테고리별 협업툴 정보")
    public record CategoryTools(

            @Schema(description = "협업툴 카테고리 ID", example = "1")
            Integer categoryId,

            @Schema(description = "확정된 협업툴 ID 목록", example = "[2, 5, 7]")
            List<Integer> confirmedToolIds
    ) {}
}