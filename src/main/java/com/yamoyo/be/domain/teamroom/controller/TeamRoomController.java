package com.yamoyo.be.domain.teamroom.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.CreateTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.service.TeamRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/team-rooms")
@RequiredArgsConstructor
public class TeamRoomController {

    private final TeamRoomService teamRoomService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreateTeamRoomResponse>> createTeamRoom(
            @Valid @RequestBody CreateTeamRoomRequest request,
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        CreateTeamRoomResponse response = teamRoomService.createTeamRoom(request,userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
