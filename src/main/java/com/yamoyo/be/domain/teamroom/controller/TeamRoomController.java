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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team-rooms")
@RequiredArgsConstructor
public class TeamRoomController {

    private final TeamRoomService teamRoomService;

    @PostMapping()
    public ResponseEntity<ApiResponse<CreateTeamRoomResponse>> createTeamRoom(
            @Valid @RequestBody CreateTeamRoomRequest request,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        CreateTeamRoomResponse response = teamRoomService.createTeamRoom(request,userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JoinTeamRoomResponse>> joinTeamRoom(
            @Valid @RequestBody JoinTeamRoomRequest request,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ){
        JoinTeamRoomResponse response = teamRoomService.joinTeamRoom(request,userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{teamRoomId}/invite-link")
    public ResponseEntity<ApiResponse<InviteLinkResponse>> issueInviteLink(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal(expression = "userId")  Long userId
    ){
        InviteLinkResponse response =  teamRoomService.issueInviteLink(teamRoomId,userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
