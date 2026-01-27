package com.yamoyo.be.domain.rule.service;

import com.yamoyo.be.domain.rule.dto.request.RuleVoteRequest;
import com.yamoyo.be.domain.rule.dto.response.RuleVoteParticipationResponse;
import com.yamoyo.be.domain.rule.dto.response.RuleVoteParticipationResponse.MemberInfo;
import com.yamoyo.be.domain.rule.entity.MemberRuleVote;
import com.yamoyo.be.domain.rule.entity.RuleTemplate;
import com.yamoyo.be.domain.rule.repository.MemberRuleVoteRepository;
import com.yamoyo.be.domain.rule.repository.RuleTemplateRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 규칙 투표 및 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleService {

    private final RuleTemplateRepository ruleTemplateRepository;
    private final MemberRuleVoteRepository memberRuleVoteRepository;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 규칙 투표 제출
     */
    @Transactional
    public void submitRuleVote(Long teamRoomId, RuleVoteRequest request, Long userId) {
        log.info("규칙 투표 제출 시작 - teamRoomId: {}, ruleId: {}, userId: {}",
                teamRoomId, request.ruleId(), userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 팀원 여부 확인
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 규칙 템플릿 조회
        RuleTemplate ruleTemplate = ruleTemplateRepository.findById(request.ruleId())
                .orElseThrow(() -> new YamoyoException(ErrorCode.RULE_NOT_FOUND));

        // 4. 중복 투표 처리 (수정 허용)
        memberRuleVoteRepository.findByMemberIdAndRuleTemplateId(member.getId(), request.ruleId())
                .ifPresentOrElse(
                        // 이미 투표한 경우 → 수정
                        existingVote -> {
                            existingVote.updateAgreement(request.toBoolean());
                            log.info("기존 투표 수정 - voteId: {}, isAgree: {}",
                                    existingVote.getId(), request.toBoolean());
                        },
                        // 첫 투표인 경우 → 생성
                        () -> {
                            MemberRuleVote newVote = MemberRuleVote.create(
                                    member, teamRoom, ruleTemplate, request.toBoolean()
                            );
                            memberRuleVoteRepository.save(newVote);
                            log.info("새 투표 생성 - memberId: {}, ruleId: {}, isAgree: {}",
                                    member.getId(), request.ruleId(), request.toBoolean());
                        }
                );

        log.info("규칙 투표 제출 완료");
    }


}