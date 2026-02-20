package com.yamoyo.be.domain.teamroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 정보 응답")
public record InviteInfoResponse(
        @Schema(description = "팀룸 ID", example = "1")
        Long teamRoomId,

        @Schema(description = "팀룸 제목", example = "프로젝트 A팀")
        String title,

        @Schema(description = "팀룸 설명", example = "캡스톤 디자인 프로젝트")
        String description,

        @Schema(description = "배너 이미지 ID", example = "3")
        Long bannerImageId,

        @Schema(description = "현재 팀원 수", example = "4")
        int currentMemberCount,

        @Schema(description = "최대 정원", example = "12")
        int maxCapacity
) {}
