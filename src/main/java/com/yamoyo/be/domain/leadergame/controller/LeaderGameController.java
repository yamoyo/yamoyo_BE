package com.yamoyo.be.domain.leadergame.controller;

import com.yamoyo.be.common.annotation.HostOnly;
import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.leadergame.dto.response.UserOnlineResponse;
import com.yamoyo.be.domain.leadergame.dto.response.VolunteerPhaseResponse;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.leadergame.service.UserStatusService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/leader-games")
@RequiredArgsConstructor
@Slf4j
public class LeaderGameController {

    private final TeamMemberRepository teamMemberRepository;
    private final UserStatusService userStatusService;
    private final LeaderGameService leaderGameService;

    /**
     * 팀룸 멤버 온라인 상태 조회
     */
    @GetMapping("/rooms/{roomId}/members")
    public ApiResponse<List<UserOnlineResponse>> getMembers(@PathVariable Long roomId) {
        List<TeamMember> members = teamMemberRepository.findByTeamRoomId(roomId);
        Set<Long> onlineUserIds = userStatusService.getOnlineUserIds(roomId);

        List<UserOnlineResponse> responses = members.stream()
                .map(member -> UserOnlineResponse.from(
                        member,
                        onlineUserIds.contains(member.getUser().getId())
                ))
                .toList();

        return ApiResponse.success(responses);
    }

    /**
     * 지원 단계 시작 (방장 전용)
     * - 전원 온라인 확인 후 VOLUNTEER 단계 시작
     * - WebSocket으로 PHASE_CHANGE 브로드캐스트
     */
    @PostMapping("/rooms/{roomId}/start-volunteer")
    @HostOnly
    public ApiResponse<VolunteerPhaseResponse> startVolunteer(
            @PathVariable Long roomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        VolunteerPhaseResponse response = leaderGameService.startVolunteerPhase(roomId, userId);
        return ApiResponse.success(response);
    }
}
