package com.yamoyo.be.domain.collabtool.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "협업툴 카테고리별 득표 현황 응답")
public record ToolVoteCountResponse(
        @Schema(description = "협업툴 카테고리 ID", example = "1")
        Integer categoryId,

        @Schema(description = "협업툴별 득표 정보 목록")
        List<ToolVoteInfo> tools
){
    @Schema(description = "협업툴 득표 정보")
    public record ToolVoteInfo(

            @Schema(description = "협업툴 ID", example = "5")
            Integer toolId,

            @Schema(description = "득표 수", example = "3")
            Long voteCount
    ) {}
}