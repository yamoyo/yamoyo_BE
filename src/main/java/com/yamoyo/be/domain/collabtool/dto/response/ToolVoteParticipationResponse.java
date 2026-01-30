package com.yamoyo.be.domain.collabtool.dto.response;

import java.util.List;

public record ToolVoteParticipationResponse(
        Integer totalMembers,
        Integer votedMembers,
        List<MemberInfo> voted,
        List<MemberInfo> notVoted
) {
    public record MemberInfo(
            Long userId,
            String userName,
            Long profileImageId
    ) {}
}
