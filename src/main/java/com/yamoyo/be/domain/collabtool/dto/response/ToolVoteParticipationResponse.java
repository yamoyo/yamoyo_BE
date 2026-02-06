package com.yamoyo.be.domain.collabtool.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "협업툴 투표 참여 현황 응답")
public record ToolVoteParticipationResponse(

        @Schema(description = "전체 팀원 수", example = "5")
        Integer totalMembers,

        @Schema(description = "투표에 참여한 팀원 수", example = "3")
        Integer votedMembers,

        @Schema(description = "투표 완료한 팀원 목록")
        List<MemberInfo> voted,

        @Schema(description = "아직 투표하지 않은 팀원 목록")
        List<MemberInfo> notVoted
) {
    @Schema(description = "팀원 정보")
    public record MemberInfo(

            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "사용자 이름", example = "홍길동")
            String userName,

            @Schema(description = "프로필 이미지 ID", example = "10")
            Long profileImageId
    ) {}
}
