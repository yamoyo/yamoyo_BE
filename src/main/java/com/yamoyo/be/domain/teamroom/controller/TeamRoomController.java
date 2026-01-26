package com.yamoyo.be.domain.teamroom.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.request.JoinTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.CreateTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.dto.response.InviteLinkResponse;
import com.yamoyo.be.domain.teamroom.dto.response.JoinTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.dto.response.TeamRoomListResponse;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.service.TeamRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-rooms")
@RequiredArgsConstructor
public class TeamRoomController {

    private final TeamRoomService teamRoomService;

    /** 팀룸 생성 */
    @PostMapping()
    public ApiResponse<CreateTeamRoomResponse> createTeamRoom(
            @Valid @RequestBody CreateTeamRoomRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        CreateTeamRoomResponse response = teamRoomService.createTeamRoom(request,userId);
        return ApiResponse.success(response);
    }

    /** 팀룸 입장 */
    @PostMapping("/join")
    public ApiResponse<JoinTeamRoomResponse> joinTeamRoom(
            @Valid @RequestBody JoinTeamRoomRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ){
        Long userId = claims.userId();
        JoinTeamRoomResponse response = teamRoomService.joinTeamRoom(request,userId);
        return ApiResponse.success(response);
    }

    /** 초대링크 생성 */
    @PostMapping("/{teamRoomId}/invite-link")
    public ApiResponse<InviteLinkResponse> issueInviteLink(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ){
        Long userId = claims.userId();
        InviteLinkResponse response =  teamRoomService.issueInviteLink(teamRoomId,userId);
        return ApiResponse.success(response);
    }

    /**
     * 팀룸 목록 조회
     * @param lifecycle ACTIVE(진행중) 또는 ARCHIVED(완료)
     */
    @GetMapping()
    public ApiResponse<List<TeamRoomListResponse>> getTeamRoomList(
            @RequestParam(name = "lifecycle", defaultValue = "ACTIVE") Lifecycle lifecycle,
            @AuthenticationPrincipal JwtTokenClaims claims
    ){
        Long userId = claims.userId();
        List<TeamRoomListResponse> response = teamRoomService.getTeamRoomList(userId, lifecycle);
        return ApiResponse.success(response);
    }

}
