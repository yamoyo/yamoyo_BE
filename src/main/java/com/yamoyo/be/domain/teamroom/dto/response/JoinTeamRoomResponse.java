package com.yamoyo.be.domain.teamroom.dto.response;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;

public record JoinTeamRoomResponse(
        JoinResult joinResult,
        Long teamRoomId,
        Long memberId
) {
    public enum JoinResult {
        JOINED,
        ENTERED,
        BANNED
    }
}
