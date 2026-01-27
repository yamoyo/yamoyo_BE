package com.yamoyo.be.domain.rule.dto.response;

import java.util.List;

public record RuleVoteParticipationResponse(
        Integer totalMembers,
        Integer votedMembers,
        List<MemberInfo> voted,
        List<MemberInfo> notVoted

) {
    public record MemberInfo(
            Long userId,
            String name,
            Long profileImageId
    ){}
}
