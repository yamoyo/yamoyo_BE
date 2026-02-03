package com.yamoyo.be.domain.teamroom.dto.response;

import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "팀 멤버 목록 응답")
public record TeamMemberListResponse(
        @Schema(description = "팀 멤버 요약 정보 목록")
        List<MemberSummary> teamMembers
) {
    @Schema(description = "멤버 요약 정보")
    public record MemberSummary(
            @Schema(description = "팀원 ID", example = "1")
            Long memberId,

            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "사용자 이름", example = "홍길동")
            String name,

            @Schema(description = "전공", example = "컴퓨터과학")
            String major,

            @Schema(description = "프로필 이미지 ID", example = "1", nullable = true)
            Long profileImageId,

            @Schema(description = "팀 내 역할", example = "LEADER")
            TeamRole role
    ){}
}
