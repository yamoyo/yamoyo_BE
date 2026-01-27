package com.yamoyo.be.domain.rule.controller;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.rule.dto.request.RuleVoteRequest;
import com.yamoyo.be.domain.rule.dto.response.RuleVoteParticipationResponse;
import com.yamoyo.be.domain.rule.service.RuleService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 규칙 투표 및 관리 Controller
 */
@RestController
@RequestMapping("/api/team-rooms/{teamRoomId}/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    /**
     * 규칙 투표 제출
     */
    @PostMapping("/vote")
    public ApiResponse<Void> submitRuleVote(
            @PathVariable Long teamRoomId,
            @Valid @RequestBody RuleVoteRequest request,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ruleService.submitRuleVote(teamRoomId, request, userId);
        return ApiResponse.success();
    }

    /**
     * 규칙 투표 참여 현황 조회
     */
    @GetMapping("/votes/participation")
    public ApiResponse<RuleVoteParticipationResponse> getRuleVoteParticipation(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        RuleVoteParticipationResponse response = ruleService.getRuleVoteParticipation(
                teamRoomId,
                userId
        );
        return ApiResponse.success(response);
    }

    /**
     * 규칙 확정 처리
     */
    @PostMapping("/confirm")
    public ApiResponse<Void> confirmRules(
            @PathVariable Long teamRoomId,
            @AuthenticationPrincipal JwtTokenClaims claims
    ) {
        Long userId = claims.userId();
        ruleService.confirmRules(teamRoomId, userId);
        return ApiResponse.success();
    }

}