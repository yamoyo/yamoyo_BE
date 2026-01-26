package com.yamoyo.be.domain.teamroom.dto.response;

import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;

import java.time.LocalDateTime;
import java.util.List;

public record TeamRoomDetailResponse(
        // 기본 정보
        Long teamRoomId,
        String title,
        String description,
        LocalDateTime deadline,
        Long bannerImageId,
        LocalDateTime createdAt,

        // 상태 정보
        Lifecycle lifecycle,
        Workflow workflow,  // 프론트 화면 분기용

        // 팀원 정보
        int memberCount,
        List<MemberSummary> members, // 팀원 프로필, 이름 표기

        // 현재 사용자 권한 (설정 버튼 노출 여부)
        TeamRole myRole
){
    public record MemberSummary(
            Long userId,            // 유저 구분
            String name,            // 이름
            Long profileImageId,    // 프로필 표기용
            TeamRole role           // 방장 표기용
    ){}
}