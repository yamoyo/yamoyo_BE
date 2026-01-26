package com.yamoyo.be.domain.teamroom.dto.response;

import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;

import java.time.LocalDateTime;
import java.util.List;

public record TeamRoomListResponse(
        Long teamRoomId,            // 팀룸 ID
        String title,               // 제목
        Long bannerImageId,         // 배너 이미지
        LocalDateTime createdAt,    // 생성일
        LocalDateTime deadline,     // 마감일
        Lifecycle status,           // 상태
        int memberCount,            // 멤버 수
        List<MemberSummary> members // 팀원 요약 정보 (프로필 이미지 표시용)
) {

    /**
     * MemberSummary
     * 팀원 요약 정보
     * - 팀룸 목록에서 팀원 아이콘 표시용
     */
    public record MemberSummary(
            Long userId,            // 사용자 ID
            Long profileImageId     // 프로필 이미지 ID
    ){}
}