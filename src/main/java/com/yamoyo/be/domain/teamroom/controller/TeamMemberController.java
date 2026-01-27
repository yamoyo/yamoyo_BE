package com.yamoyo.be.domain.teamroom.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.teamroom.dto.request.ChangeLeaderRequest;
import com.yamoyo.be.domain.teamroom.service.TeamMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    /**
     * 팀룸 나가기 (스스로)
     */
    @DeleteMapping("/members/me")
    public ApiResponse<Void> leaveTeamRoom(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamMemberService.leaveTeamRoom(teamRoomId, userId);
        return ApiResponse.success();
    }

    /**
     * 팀원 강퇴
     * @param memberId 강퇴 대상 멤버 ID
     */
    @DeleteMapping("/members/{memberId}")
    public ApiResponse<Void> kickMember(
            @PathVariable Long teamRoomId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamMemberService.kickMember(teamRoomId, userId, memberId);
        return ApiResponse.success();
    }

    /**
     * 팀장 변경 (명시적 위임)
     */
    @PutMapping("/leader")
    public ApiResponse<Void> changeLeader(
            @PathVariable Long teamRoomId,
            @Valid @RequestBody ChangeLeaderRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamMemberService.changeLeader(teamRoomId, userId, request.newLeaderMemberId());
        return ApiResponse.success();
    }
}