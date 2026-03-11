package com.yamoyo.be.domain.collabtool.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 협업툴 투표 내역 응답")
public record MyToolVoteResponse(

        @Schema(description = "투표 완료 여부", example = "true")
        boolean voted,

        @Schema(description = "카테고리별 내 투표 내역 (미투표 시 빈 배열)")
        List<CategoryVote> votes
) {
    @Schema(description = "카테고리별 투표 내역")
    public record CategoryVote(

            @Schema(description = "카테고리 ID", example = "1")
            Integer categoryId,

            @Schema(description = "선택한 협업툴 ID 목록", example = "[1, 3, 5]")
            List<Integer> toolIds
    ) {}
}
