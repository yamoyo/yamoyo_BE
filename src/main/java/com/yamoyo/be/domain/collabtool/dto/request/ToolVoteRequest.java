package com.yamoyo.be.domain.collabtool.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "협업툴 투표 일괄 제출 요청")
public record ToolVoteRequest (
        @Schema(description = "카테고리별 투표 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "투표 목록은 필수입니다")
        @NotEmpty(message = "투표 목록은 비어 있을 수 없습니다")
        @Valid
        List<CategoryToolVote> toolVotes
){
    @Schema(description = "카테고리별 협업툴 투표")
    public record CategoryToolVote(
            @Schema(description = "협업툴 카테고리 ID", example = "1",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "카테고리 ID는 필수입니다")
            Integer categoryId,

            @Schema(description = "선택한 툴 ID 목록", example = "[1, 3, 5]",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "툴 ID 목록은 필수입니다")
            @NotEmpty(message = "툴 ID 목록은 비어 있을 수 없습니다")
            List<Integer> toolIds
    ){}
}