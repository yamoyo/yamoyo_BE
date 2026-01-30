package com.yamoyo.be.domain.leadergame.dto.response;

import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;

public record UserOnlineResponse(
        Long userId,
        String name,
        TeamRole role,
        String major,
        Long profileImageId,
        String status
) {
    public static UserOnlineResponse from(TeamMember member, boolean status) {
        return new UserOnlineResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getTeamRole(),
                member.getUser().getMajor(),
                member.getUser().getProfileImageId(),
                status ? "ONLINE" : "OFFLINE");
    }
}
