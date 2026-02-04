package com.yamoyo.be.domain.leadergame.controller;

import com.yamoyo.be.common.annotation.HostOnly;
import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.leadergame.dto.response.UserOnlineResponse;
import com.yamoyo.be.domain.leadergame.dto.response.VolunteerPhaseResponse;
import com.yamoyo.be.domain.leadergame.service.GameStateRedisService;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "Leader Game", description = "팀장 선정 게임 API")
@RestController
@RequestMapping("/api/leader-games")
@RequiredArgsConstructor
@Slf4j
public class LeaderGameController {

    private final TeamMemberRepository teamMemberRepository;
    private final GameStateRedisService gameStateRedisService;
    private final LeaderGameService leaderGameService;

    @Operation(summary = "팀룸 멤버 온라인 상태 조회", description = "팀룸 멤버들의 온라인/오프라인 상태를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸을 찾을 수 없음")
    })
    @GetMapping("/rooms/{roomId}/members")
    public ApiResponse<List<UserOnlineResponse>> getMembers(
            @Parameter(description = "팀룸 ID") @PathVariable Long roomId) {
        List<TeamMember> members = teamMemberRepository.findByTeamRoomId(roomId);
        Set<Long> onlineUserIds = gameStateRedisService.getConnectedUsers(roomId);

        List<UserOnlineResponse> responses = members.stream()
                .map(member -> UserOnlineResponse.from(
                        member,
                        onlineUserIds.contains(member.getUser().getId())
                ))
                .toList();

        return ApiResponse.success(responses);
    }

    @Operation(summary = "지원 단계 시작", description = "팀장 지원 단계를 시작합니다. 전원 온라인 확인 후 VOLUNTEER 단계가 시작되며, WebSocket으로 PHASE_CHANGE가 브로드캐스트됩니다. (방장 전용)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지원 단계 시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "전원 온라인 상태가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방장 권한 필요")
    })
    @PostMapping("/rooms/{roomId}/start-volunteer")
    @HostOnly
    public ApiResponse<VolunteerPhaseResponse> startVolunteer(
            @Parameter(description = "팀룸 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        VolunteerPhaseResponse response = leaderGameService.startVolunteerPhase(roomId, userId);
        return ApiResponse.success(response);
    }
}
