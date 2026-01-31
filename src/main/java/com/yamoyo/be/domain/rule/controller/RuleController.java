package com.yamoyo.be.domain.rule.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.rule.dto.request.TeamRuleRequest;
import com.yamoyo.be.domain.rule.dto.request.RuleVoteRequest;
import com.yamoyo.be.domain.rule.dto.response.RuleVoteParticipationResponse;
import com.yamoyo.be.domain.rule.dto.response.TeamRulesResponse;
import com.yamoyo.be.domain.rule.service.RuleService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "팀 규칙", description = "팀 규칙 설정 API")
@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @Operation(summary = "규칙 투표", description = "팀룸의 규칙을 투표합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "투표 제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 규칙을 찾을 수 없음")
    })
    @PostMapping("/vote")
    public ApiResponse<Void> submitRuleVote(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Valid @RequestBody RuleVoteRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims

    ) {
        Long userId = claims.userId();
        ruleService.submitRuleVote(teamRoomId, request, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "규칙 투표 참여 현황 조회", description = "팀룸 내 규칙 투표의 참여 현황을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 투표를 찾을 수 없음")
    })
    @GetMapping("/votes/participation")
    public ApiResponse<RuleVoteParticipationResponse> getRuleVoteParticipation(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        RuleVoteParticipationResponse response = ruleService.getRuleVoteParticipation(
                teamRoomId,
                userId
        );
        return ApiResponse.success(response);
    }

    @Operation(summary = "확정된 규칙 조회", description = "팀룸에서 확정된 규칙 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 규칙을 찾을 수 없음")
    })
    @GetMapping
    public ApiResponse<TeamRulesResponse> getTeamRules(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        TeamRulesResponse response = ruleService.getTeamRules(teamRoomId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "규칙 추가", description = "팀룸에 규칙을 추가합니다. (팀장만 추가 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 없음")
    })
    @PostMapping
    public ApiResponse<Void> addTeamRule(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Valid @RequestBody TeamRuleRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ruleService.addTeamRule(teamRoomId, request, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "규칙 수정", description = "팀룸의 규칙을 수정합니다. (팀장만 수정 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 규칙을 찾을 수 없음")
    })
    @PutMapping("/{teamRuleId}")
    public ApiResponse<Void> updateTeamRule(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @PathVariable Long teamRuleId,
            @Valid @RequestBody TeamRuleRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ruleService.updateTeamRule(teamRoomId, teamRuleId, request, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "규칙 삭제", description = "팀룸의 규칙을 삭제합니다. (팀장만 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팀룸 또는 규칙을 찾을 수 없음")
    })
    @DeleteMapping("/{teamRuleId}")
    public ApiResponse<Void> deleteTeamRule(
            @Parameter(description = "팀룸 ID", required = true) @PathVariable Long teamRoomId,
            @Parameter(description = "규칙 ID", required = true, example = "5") @PathVariable Long teamRuleId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ruleService.deleteTeamRule(teamRoomId, teamRuleId, userId);
        return ApiResponse.success();
    }
}