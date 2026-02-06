package com.yamoyo.be.domain.teamroom.dto.response;

import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "팀룸 목록 응답")
public record TeamRoomListResponse(
        @Schema(description = "팀룸 ID", example = "1")
        Long teamRoomId,

        @Schema(description = "팀룸 제목", example = "알고리즘 스터디")
        String title,

        @Schema(description = "배너 이미지 ID", example = "1", nullable = true)
        Long bannerImageId,

        @Schema(description = "생성일시", example = "2026-01-01T00:00:00")
        LocalDateTime createdAt,

        @Schema(description = "마감일시", example = "2026-02-28T23:59:59")
        LocalDateTime deadline,

        @Schema(description = "팀룸 상태", example = "ACTIVE")
        Lifecycle status,

        @Schema(description = "멤버 수", example = "5")
        int memberCount,

        @Schema(description = "팀원 요약 정보 (프로필 이미지 표시용)")
        List<MemberSummary> members
) {

    @Schema(description = "멤버 요약 정보")
    public record MemberSummary(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "프로필 이미지 ID", example = "1", nullable = true)
            Long profileImageId
    ){}
}