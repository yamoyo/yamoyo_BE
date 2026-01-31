package com.yamoyo.be.domain.collabtool.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.collabtool.dto.request.ApproveProposalRequest;
import com.yamoyo.be.domain.collabtool.dto.request.ProposeToolRequest;
import com.yamoyo.be.domain.collabtool.dto.request.ToolVoteRequest;
import com.yamoyo.be.domain.collabtool.dto.response.ConfirmedToolsResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ProposalDetailResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteCountResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteParticipationResponse;
import com.yamoyo.be.domain.collabtool.service.ToolService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "협업툴", description = "협업툴 설정 API")
@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;

    @Operation(summary = "협업툴 투표 일괄 제출", description = "협업툴을 여러 개 선택하여 한 번에 투표를 제출합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "투표 제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 협업툴 정보를 찾을 수 없음")
    })
    @PostMapping("/votes")
    public ApiResponse<Void> submitAllToolVotes(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Valid @RequestBody ToolVoteRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        toolService.submitAllToolVotes(teamRoomId, userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "카테고리별 득표 현황 조회", description = "협업툴 카테고리별 득표 현황을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 카테고리 정보를 찾을 수 없음")
    })
    @GetMapping("/votes/category/{categoryId}")
    public ApiResponse<ToolVoteCountResponse> getVoteCountByCategory(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(description = "협업툴 카테고리 ID", required = true, example = "1") @PathVariable Integer categoryId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ToolVoteCountResponse response = toolService.getVoteCountByCategory(
                teamRoomId,
                userId,
                categoryId
        );
        return ApiResponse.success(response);
    }

    @Operation(summary = "협업툴 투표 참여 현황 조회", description = "협업툴 투표 참여 현황(투표 완료/미완료)을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 투표 정보를 찾을 수 없음")
    })
    @GetMapping("/votes/participation")
    public ApiResponse<ToolVoteParticipationResponse> getVotedMemberParticipation(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ToolVoteParticipationResponse response = toolService.getVotedMemberParticipation(teamRoomId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "확정된 협업툴 조회", description = "팀룸에서 확정된 협업툴 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 정보를 찾을 수 없음")
    })
    @GetMapping
    public ApiResponse<ConfirmedToolsResponse> getConfirmedTools(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ConfirmedToolsResponse response = toolService.getConfirmedTools(teamRoomId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "협업툴 삭제", description = "팀룸의 협업툴을 삭제합니다. (팀장만 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 협업툴을 찾을 수 없음")
    })
    @DeleteMapping("/{teamToolId}")
    public ApiResponse<Void> deleteTeamTool(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(description = "팀 협업툴 ID", required = true, example = "10") @PathVariable Long teamToolId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        toolService.deleteTeamTool(teamRoomId, teamToolId, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "협업툴 제안", description = "팀룸에 새로운 협업툴을 제안합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제안 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸을 찾을 수 없음")
    })
    @PostMapping("/proposals")
    public ApiResponse<Void> proposeTool(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Valid @RequestBody ProposeToolRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        toolService.proposeTool(teamRoomId, userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "협업툴 제안 승인/반려", description = "협업툴 제안을 승인 또는 반려합니다. (팀장만 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 제안을 찾을 수 없음")
    })
    @PutMapping("/proposals/{proposalId}")
    public ApiResponse<Void> approveOrRejectProposal(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(description = "제안 ID", required = true, example = "5")@PathVariable Long proposalId,
            @Valid @RequestBody ApproveProposalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "협업툴 제안 상세 조회", description = "협업툴 제안 단건 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 제안을 찾을 수 없음")
    })
    @GetMapping("/proposals/{proposalId}")
    public ApiResponse<ProposalDetailResponse> getProposalDetail(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(description = "제안 ID", required = true, example = "5") @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ProposalDetailResponse response = toolService.getProposalDetail(teamRoomId, proposalId, userId);
        return ApiResponse.success(response);
    }
}