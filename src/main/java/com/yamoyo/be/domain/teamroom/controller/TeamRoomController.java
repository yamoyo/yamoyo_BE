package com.yamoyo.be.domain.teamroom.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.request.JoinTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.*;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.service.TeamRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "팀룸", description = "팀룸 관리 API")
@RestController
@RequestMapping("/api/team-rooms")
@RequiredArgsConstructor
public class TeamRoomController {

    private final TeamRoomService teamRoomService;

    @Operation(summary = "팀룸 생성", description = "새로운 팀룸을 생성하고, 기본 초대 토큰을 발급합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")

    })
    @PostMapping()
    public ApiResponse<CreateTeamRoomResponse> createTeamRoom(
            @Valid @RequestBody CreateTeamRoomRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        CreateTeamRoomResponse response = teamRoomService.createTeamRoom(request,userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "팀룸 입장", description = "초대 토큰을 사용하여 팀룸에 입장합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청/정원초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "입장 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "초대링크 만료/무효")

    })
    @PostMapping("/join")
    public ApiResponse<JoinTeamRoomResponse> joinTeamRoom(
            @Valid @RequestBody JoinTeamRoomRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ){
        Long userId = claims.userId();
        JoinTeamRoomResponse response = teamRoomService.joinTeamRoom(request,userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "초대 링크 생성", description = "팀룸의 새로운 초대 링크 토큰을 발급합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 없음")

    })
    @PostMapping("/{teamRoomId}/invite-link")
    public ApiResponse<InviteLinkResponse> issueInviteLink(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ){
        Long userId = claims.userId();
        InviteLinkResponse response =  teamRoomService.issueInviteLink(teamRoomId,userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "팀룸 목록 조회", description = "사용자가 속한 팀룸 목록을 조회합니다. lifecycle 파라미터로 진행중/완료 필터링이 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")

    })
    @GetMapping()
    public ApiResponse<List<TeamRoomListResponse>> getTeamRoomList(
            @Parameter(description = "팀룸 상태 (ACTIVE: 진행중, ARCHIVED: 완료)", example = "ACTIVE")
            @RequestParam(name = "lifecycle", defaultValue = "ACTIVE") Lifecycle lifecycle,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ){
        Long userId = claims.userId();
        List<TeamRoomListResponse> response = teamRoomService.getTeamRoomList(userId, lifecycle);
        return ApiResponse.success(response);
    }

    @Operation(summary = "팀룸 상세 조회", description = "팀룸의 상세 정보를 조회합니다. 팀원 목록, 진행 상태 등이 포함됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 없음")

    })
    @GetMapping("/{teamRoomId}")
    public ApiResponse<TeamRoomDetailResponse> getTeamRoomDetail(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        TeamRoomDetailResponse response = teamRoomService.getTeamRoomDetail(teamRoomId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "팀룸 수정", description = "팀룸의 제목, 설명, 마감일, 배너 이미지를 수정합니다.(팀장만 수정 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 없음")
    })
    @PutMapping("/{teamRoomId}")
    public ApiResponse<Void> updateTeamRoom(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Valid @RequestBody CreateTeamRoomRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamRoomService.updateTeamRoom(teamRoomId, request, userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "팀룸 삭제", description = "팀룸을 삭제합니다.(팀장만 삭제 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 없음")
    })
    @DeleteMapping("/{teamRoomId}")
    public ApiResponse<Void> deleteTeamRoom(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        teamRoomService.deleteTeamRoom(teamRoomId, userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "초대 정보 조회", description = "초대 토큰으로 팀룸 정보를 조회합니다. 비로그인 사용자도 조회 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "초대링크 만료/무효")
    })
    @GetMapping("/invite-info")
    public ApiResponse<InviteInfoResponse> getInviteInfo(
            @Parameter(description = "초대 토큰", required = true) @RequestParam String token
    ) {
        InviteInfoResponse response = teamRoomService.getInviteInfo(token);
        return ApiResponse.success(response);
    }
}
