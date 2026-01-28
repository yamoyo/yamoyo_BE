package com.yamoyo.be.domain.collabtool.dto.response;

import java.util.List;

public record ToolVoteCountResponse(
        Integer categoryId,
        List<ToolVoteInfo> tools
){
    public record ToolVoteInfo(
            Integer toolId,
            Long voteCount
    ) {}
}