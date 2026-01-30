package com.yamoyo.be.domain.leadergame.dto.message;

import java.util.Set;

public record VoteStatusPayload(
        Set<Long> votedUserIds,
        Set<Long> unvotedUserIds,
        Set<Long> volunteerIds,
        int totalCount,
        int votedCount
) {
    public static VoteStatusPayload of(Set<Long> votedUserIds,  Set<Long> unvotedUserIds, Set<Long> volunteerIds, int totalCount) {
        return new VoteStatusPayload(votedUserIds, unvotedUserIds, volunteerIds, totalCount, votedUserIds.size());
    }
}
