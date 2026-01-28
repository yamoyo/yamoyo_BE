package com.yamoyo.be.domain.collabtool.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.collabtool.dto.request.ToolVoteRequest;
import com.yamoyo.be.domain.collabtool.dto.response.ConfirmedToolsResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteCountResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteParticipationResponse;
import com.yamoyo.be.domain.collabtool.service.ToolService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;

    /**
     * 협업툴 투표 일괄 제출
     */
    @PostMapping("/votes")
    public ApiResponse<Void> submitAllToolVotes(
            @PathVariable Long teamRoomId,
            @Valid @RequestBody ToolVoteRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        toolService.submitAllToolVotes(teamRoomId, userId, request);
        return ApiResponse.success();
    }

    /**
     * 카테고리별 득표 현황 조회
     */
    @GetMapping("/votes/category/{categoryId}")
    public ApiResponse<ToolVoteCountResponse> getVoteCountByCategory(
            @PathVariable Long teamRoomId,
            @PathVariable Integer categoryId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ToolVoteCountResponse response = toolService.getVoteCountByCategory(
                teamRoomId,
                userId,
                categoryId
        );
        return ApiResponse.success(response);
    }

    /**
     * 투표 참여 현황 조회
     */
    @GetMapping("/votes/participation")
    public ApiResponse<ToolVoteParticipationResponse> getVotedMemberParticipation(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ToolVoteParticipationResponse response = toolService.getVotedMemberParticipation(teamRoomId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 확정된 협업툴 조회
     */
    @GetMapping
    public ApiResponse<ConfirmedToolsResponse> getConfirmedTools(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ConfirmedToolsResponse response = toolService.getConfirmedTools(teamRoomId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 협업툴 삭제 (팀장만)
     */
    @DeleteMapping("/{teamToolId}")
    public ApiResponse<Void> deleteTeamTool(
            @PathVariable Long teamRoomId,
            @PathVariable Long teamToolId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        toolService.deleteTeamTool(teamRoomId, teamToolId, userId);
        return ApiResponse.success();
    }
}