package com.yamoyo.be.domain.teamroom.dto.response;

import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "팀룸 상세 응답")
public record TeamRoomDetailResponse(
        @Schema(description = "팀룸 ID", example = "1")
        Long teamRoomId,

        @Schema(description = "팀룸 제목", example = "알고리즘 스터디")
        String title,

        @Schema(description = "팀룸 설명", example = "매일 알고리즘 1문제씩 풀기", nullable = true)
        String description,

        @Schema(description = "마감일시", example = "2026-02-28T23:59:59")
        LocalDateTime deadline,

        @Schema(description = "배너 이미지 ID", example = "1", nullable = true)
        Long bannerImageId,

        @Schema(description = "생성일시", example = "2026-01-01T00:00:00")
        LocalDateTime createdAt,

        @Schema(description = "생명주기 상태", example = "ACTIVE")
        Lifecycle lifecycle,

        @Schema(description = "워크플로우 상태 (화면 분기용)", example = "SETUP")
        Workflow workflow,

        @Schema(description = "멤버 수", example = "5")
        int memberCount,

        @Schema(description = "팀원 정보 목록")
        List<MemberSummary> members,

        @Schema(description = "현재 사용자의 역할 (설정 버튼 노출 여부)", example = "LEADER")
        TeamRole myRole,

        @Schema(description = "Setup 시작 시각 SETUP 단계에서만 존재)", example = "2026-01-01T06:00:00", nullable = true)
        LocalDateTime setupCreatedAt
){
    @Schema(description = "멤버 상세 정보")
    public record MemberSummary(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "사용자 이름", example = "홍길동")
            String name,

            @Schema(description = "프로필 이미지 ID", example = "1", nullable = true)
            Long profileImageId,

            @Schema(description = "팀 내 역할", example = "LEADER")
            TeamRole role
    ){}
}