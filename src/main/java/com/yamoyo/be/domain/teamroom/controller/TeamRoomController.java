package com.yamoyo.be.domain.teamroom.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.request.JoinTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.CreateTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.dto.response.InviteLinkResponse;
import com.yamoyo.be.domain.teamroom.dto.response.JoinTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.service.TeamRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team-rooms")
@RequiredArgsConstructor
public class TeamRoomController {

    private final TeamRoomService teamRoomService;

    @PostMapping()
    public ApiResponse<CreateTeamRoomResponse> createTeamRoom(
            @Valid @RequestBody CreateTeamRoomRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ) {
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        CreateTeamRoomResponse response = teamRoomService.createTeamRoom(request,userId);
        return ApiResponse.success(response);
    }

    @PostMapping("/join")
    public ApiResponse<JoinTeamRoomResponse> joinTeamRoom(
            @Valid @RequestBody JoinTeamRoomRequest request,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ){
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        JoinTeamRoomResponse response = teamRoomService.joinTeamRoom(request,userId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{teamRoomId}/invite-link")
    public ApiResponse<InviteLinkResponse> issueInviteLink(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal OAuth2User oAuth2User
    ){
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        InviteLinkResponse response =  teamRoomService.issueInviteLink(teamRoomId,userId);
        return ApiResponse.success(response);
    }

}
