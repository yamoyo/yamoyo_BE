package com.yamoyo.be.domain.teamroom.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.teamroom.dto.request.ChangeLeaderRequest;
import com.yamoyo.be.domain.teamroom.service.TeamMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "팀원", description = "팀원 관리 API")
@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    @Operation(summary = "팀룸 나가기", description = "본인이 속한 팀룸에서 나갑니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "나가기 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 멤버 정보를 찾을 수 없음")
    })
    @DeleteMapping("/members/me")
    public ApiResponse<Void> leaveTeamRoom(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamMemberService.leaveTeamRoom(teamRoomId, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "팀원 강퇴", description = "팀룸에서 특정 팀원을 강퇴합니다.(팀장만 강퇴 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "강퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 대상 멤버를 찾을 수 없음")
    })
    @DeleteMapping("/members/{memberId}")
    public ApiResponse<Void> kickMember(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(description = "강퇴 대상 멤버 ID", required = true, example = "10") @PathVariable Long memberId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamMemberService.kickMember(teamRoomId, userId, memberId);
        return ApiResponse.success();
    }

    @Operation(summary = "팀장 변경", description = "팀룸의 팀장(방장)을 다른 멤버에게 위임합니다.(팀장만 위임 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "팀장 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 대상 멤버를 찾을 수 없음")
    })
    @PutMapping("/leader")
    public ApiResponse<Void> changeLeader(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Valid @RequestBody ChangeLeaderRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamMemberService.changeLeader(teamRoomId, userId, request.newLeaderMemberId());
        return ApiResponse.success();
    }
}