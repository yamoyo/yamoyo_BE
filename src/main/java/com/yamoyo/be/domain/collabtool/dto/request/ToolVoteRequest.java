package com.yamoyo.be.domain.collabtool.dto.request;

import java.util.List;

public record ToolVoteRequest (
        List<CategoryToolVote> toolVotes
){
    /**
     * 카테고리별 협업툴 투표
     */
    public record CategoryToolVote(
            Integer categoryId,
            List<Integer> toolIds
    ){}
}