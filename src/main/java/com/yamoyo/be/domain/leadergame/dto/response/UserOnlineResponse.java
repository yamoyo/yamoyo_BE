package com.yamoyo.be.domain.leadergame.dto.response;

import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 온라인 상태 응답")
public record UserOnlineResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "팀 내 역할", example = "MEMBER")
        TeamRole role,

        @Schema(description = "전공", example = "컴퓨터공학")
        String major,

        @Schema(description = "프로필 이미지 ID", example = "1")
        Long profileImageId,

        @Schema(description = "온라인 상태 (ONLINE/OFFLINE)", example = "ONLINE")
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
