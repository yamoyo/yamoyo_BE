package com.yamoyo.be.domain.rule.service;

import com.yamoyo.be.domain.rule.dto.request.TeamRuleRequest;
import com.yamoyo.be.domain.rule.dto.request.RuleVoteRequest;
import com.yamoyo.be.domain.rule.dto.response.RuleVoteParticipationResponse;
import com.yamoyo.be.domain.rule.dto.response.RuleVoteParticipationResponse.MemberInfo;
import com.yamoyo.be.domain.rule.dto.response.TeamRulesResponse;
import com.yamoyo.be.domain.rule.entity.MemberRuleVote;
import com.yamoyo.be.domain.rule.entity.RuleTemplate;
import com.yamoyo.be.domain.rule.entity.TeamRule;
import com.yamoyo.be.domain.rule.repository.MemberRuleVoteRepository;
import com.yamoyo.be.domain.rule.repository.RuleTemplateRepository;
import com.yamoyo.be.domain.rule.repository.TeamRuleRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.scheduler.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
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
    private final TeamRuleRepository teamRuleRepository;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoomSetupRepository setupRepository;

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

        // 5. 전원 투표 완료 체크
        long totalMembers = teamMemberRepository.countByTeamRoomId(teamRoomId);
        long votedMembers = memberRuleVoteRepository.countDistinctMembersByTeamRoom(teamRoomId);

        log.info("투표 현황 - 전체: {}, 투표완료: {}", totalMembers, votedMembers);

        if (totalMembers == votedMembers) {
            log.info("전원 투표 완료 - 규칙 자동 확정");
            confirmRules(teamRoomId);
        }

        log.info("규칙 투표 제출 완료");
    }

    /**
     * 규칙 투표 참여 현황 조회
     * @return 투표 참여 현황
     */
    public RuleVoteParticipationResponse getRuleVoteParticipation(Long teamRoomId, Long userId) {
        log.info("규칙 투표 참여 현황 조회 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 팀원인지 확인
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 전체 팀원 조회
        List<TeamMember> allMembers = teamMemberRepository.findByTeamRoomId(teamRoomId);

        // 4. 투표한 팀원 ID 목록 조회 (중복 제거)
        List<MemberRuleVote> votes = memberRuleVoteRepository.findByTeamRoomId(teamRoomId);
        Set<Long> votedMemberIds = votes.stream()
                .map(vote -> vote.getMember().getId())
                .collect(Collectors.toSet());

        // 5. 투표 완료/미완료 팀원 분류
        List<MemberInfo> voted = new ArrayList<>();
        List<MemberInfo> notVoted = new ArrayList<>();

        for (TeamMember member : allMembers) {
            User user = member.getUser();
            MemberInfo memberInfo =
                    new MemberInfo(
                            user.getId(),
                            user.getName(),
                            user.getProfileImageId()
                    );

            if (votedMemberIds.contains(member.getId())) {
                voted.add(memberInfo);
            } else {
                notVoted.add(memberInfo);
            }
        }

        log.info("규칙 투표 참여 현황 조회 완료 - 전체: {}, 투표완료: {}", allMembers.size(), voted.size());

        return new RuleVoteParticipationResponse(
                allMembers.size(),
                voted.size(),
                voted,
                notVoted
        );
    }

    /**
     * 규칙 확정 처리
     * - 과반수 이상 동의한 규칙을 팀 규칙으로 확정
     * - 동률 시 팀장 투표에 우선순위 부여
     */
    @Transactional
    public void confirmRules(Long teamRoomId) {
        log.info("규칙 확정 처리 시작 - teamRoomId: {}", teamRoomId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 모든 투표 조회
        List<MemberRuleVote> allVotes = memberRuleVoteRepository.findByTeamRoomId(teamRoomId);

        // 3. 투표한 팀원 수 계산
        long votedMembers = allVotes.stream()
                .map(vote -> vote.getMember().getId())
                .distinct()
                .count();

        // 4. 과반수 = 투표한 사람 기준
        long majorityThreshold = (votedMembers / 2) + 1;

        // 5. 규칙별 동의 득표 수 집계
        Map<Long, Long> agreeCountByRule = allVotes.stream()
                .filter(MemberRuleVote::getIsAgree)
                .collect(Collectors.groupingBy(
                        vote -> vote.getRuleTemplate().getId(),
                        Collectors.counting()
                ));

        // 6. 과반수 이상 득표한 규칙 필터링
        List<Long> confirmedRuleIds = agreeCountByRule.entrySet().stream()
                .filter(entry -> entry.getValue() >= majorityThreshold)
                .map(Map.Entry::getKey)
                .toList();

        // 7. 팀 규칙으로 저장
        for (Long ruleId : confirmedRuleIds) {
            RuleTemplate template = ruleTemplateRepository.findById(ruleId)
                    .orElseThrow(() -> new YamoyoException(ErrorCode.RULE_NOT_FOUND));

            TeamRule teamRule = TeamRule.create(teamRoom, template.getContent());
            teamRuleRepository.save(teamRule);

            log.info("규칙 확정 - ruleId: {}, content: {}", ruleId, template.getContent());
        }

        // Setup 상태 업데이트
        TeamRoomSetup setup = setupRepository.findByTeamRoomId(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.SETUP_NOT_FOUND));
        setup.completeRuleSetup();
        log.info("규칙 확정 완료 - 총 {}개 규칙 확정", confirmedRuleIds.size());
    }

    /**
     * 확정된 규칙 조회
     * @return 확정된 규칙 목록
     */
    public TeamRulesResponse getTeamRules(Long teamRoomId, Long userId) {
        log.info("확정된 규칙 조회 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 팀원인지 확인
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 확정된 규칙 조회
        List<TeamRule> teamRules = teamRuleRepository.findByTeamRoomId(teamRoomId);

        // 4. DTO 변환
        List<TeamRulesResponse.TeamRuleInfo> teamRuleInfos = teamRules.stream()
                .map(rule -> new TeamRulesResponse.TeamRuleInfo(
                        rule.getId(),
                        rule.getContent()
                ))
                .toList();

        log.info("확정된 규칙 조회 완료 - 총 {}개", teamRuleInfos.size());

        return new TeamRulesResponse(teamRuleInfos);
    }

    /**
     * 규칙 추가 (팀장만)
     */
    @Transactional
    public void addTeamRule(Long teamRoomId, TeamRuleRequest request, Long userId) {
        log.info("규칙 추가 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 팀장 권한 확인
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        if (!member.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 3. 규칙 생성
        TeamRule teamRule = TeamRule.create(teamRoom, request.content());
        teamRuleRepository.save(teamRule);

        log.info("규칙 추가 완료 - teamRuleId: {}", teamRule.getId());
    }

    /**
     * 규칙 수정 (팀장만)
     *
     * @param teamRoomId 팀룸 ID
     * @param teamRuleId 규칙 ID
     * @param request 수정할 내용
     * @param userId 요청자 ID
     */
    @Transactional
    public void updateTeamRule(Long teamRoomId, Long teamRuleId, TeamRuleRequest request, Long userId) {
        log.info("규칙 수정 시작 - teamRoomId: {}, teamRuleId: {}, userId: {}",
                teamRoomId, teamRuleId, userId);

        // 1. 팀장 권한 확인
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        if (!member.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 2. 규칙 조회
        TeamRule teamRule = teamRuleRepository.findById(teamRuleId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.RULE_NOT_FOUND));

        // 3. 규칙이 해당 팀룸 소속인지 확인
        if (!teamRule.getTeamRoom().getId().equals(teamRoomId)) {
            throw new YamoyoException(ErrorCode.RULE_NOT_FOUND);
        }

        // 4. 규칙 수정
        teamRule.updateContent(request.content());

        log.info("규칙 수정 완료 - teamRuleId: {}", teamRuleId);
    }

    /**
     * 규칙 삭제 (팀장만)
     *
     * @param teamRoomId 팀룸 ID
     * @param teamRuleId 규칙 ID
     * @param userId 요청자 ID
     */
    @Transactional
    public void deleteTeamRule(Long teamRoomId, Long teamRuleId, Long userId) {
        log.info("규칙 삭제 시작 - teamRoomId: {}, teamRuleId: {}, userId: {}",
                teamRoomId, teamRuleId, userId);

        // 1. 팀장 권한 확인
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        if (!member.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 2. 규칙 조회
        TeamRule teamRule = teamRuleRepository.findById(teamRuleId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.RULE_NOT_FOUND));

        // 3. 규칙이 해당 팀룸 소속인지 확인
        if (!teamRule.getTeamRoom().getId().equals(teamRoomId)) {
            throw new YamoyoException(ErrorCode.RULE_NOT_FOUND);
        }

        // 4. 규칙 삭제
        teamRuleRepository.delete(teamRule);

        log.info("규칙 삭제 완료 - teamRuleId: {}", teamRuleId);
    }
}