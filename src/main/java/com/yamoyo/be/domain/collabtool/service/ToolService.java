package com.yamoyo.be.domain.collabtool.service;

import com.yamoyo.be.domain.collabtool.dto.request.ToolVoteRequest;
import com.yamoyo.be.domain.collabtool.dto.response.ConfirmedToolsResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteCountResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteParticipationResponse;
import com.yamoyo.be.domain.collabtool.entity.MemberToolVote;
import com.yamoyo.be.domain.collabtool.entity.TeamTool;
import com.yamoyo.be.domain.collabtool.repository.MemberToolVoteRepository;
import com.yamoyo.be.domain.collabtool.repository.TeamToolRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ToolService {

    private final MemberToolVoteRepository toolVoteRepository;
    private final TeamToolRepository teamToolRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoomRepository teamRoomRepository;

    /**
     * 협업툴 투표 일괄 제출
     * - 모든 카테고리의 협업툴을 한번에 제출
     * - 재투표 불가
     */
    @Transactional
    public void submitAllToolVotes(Long teamRoomId, Long userId, ToolVoteRequest request) {
        log.info("협업툴 투표 일괄 제출 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 팀원 여부 확인
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 중복 투표 방지 (이미 일괄 제출한 경우)
        if (toolVoteRepository.existsByMemberId(member.getId())) {
            throw new YamoyoException(ErrorCode.ALREADY_VOTED);
        }

        // 4. 모든 카테고리 투표를 하나의 리스트로 생성
        List<MemberToolVote> allVotes = new ArrayList<>();

        for (ToolVoteRequest.CategoryToolVote categoryVote : request.toolVotes()) {
            List<MemberToolVote> votes = categoryVote.toolIds().stream()
                    .map(toolId -> MemberToolVote.create(
                            member.getId(),
                            teamRoomId,
                            categoryVote.categoryId(),
                            toolId
                    ))
                    .toList();

            allVotes.addAll(votes);

            log.info("카테고리 {} 투표 생성 - 선택한 툴 개수: {}",
                    categoryVote.categoryId(), votes.size());
        }

        // 5. 한 트랜잭션에서 일괄 저장
        toolVoteRepository.saveAll(allVotes);

        // 6. 전원 투표 완료 체크
        long totalMembers = teamMemberRepository.countByTeamRoomId(teamRoomId);
        long votedMembers = toolVoteRepository.findVotedMemberIds(teamRoomId).size();

        log.info("투표 현황 - 전체: {}, 투표완료: {}", totalMembers, votedMembers);

        if (totalMembers == votedMembers) {
            log.info("전원 투표 완료 - 협업툴 자동 확정");
            confirmTools(teamRoomId);
        }

        log.info("협업툴 투표 일괄 제출 완료 - 총 {}개 투표 저장", allVotes.size());
    }

    /**
     * 카테고리별 득표 현황 조회
     * - 특정 카테고리의 각 협업툴이 받은 투표 수 집계
     */
    public ToolVoteCountResponse getVoteCountByCategory(Long teamRoomId, Long userId, Integer categoryId) {
        log.info("카테고리별 득표 현황 조회 시작 - teamRoomId: {}, categoryId: {}", teamRoomId, categoryId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 팀원인지 확인
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 카테고리별 득표 현황 조회
        List<Object[]> results = toolVoteRepository.countVotesByCategory(teamRoomId, categoryId);

        // 4. DTO 변환
        List<ToolVoteCountResponse.ToolVoteInfo> tools = results.stream()
                .map(row -> new ToolVoteCountResponse.ToolVoteInfo(
                        (Integer) row[0],  // toolId
                        (Long) row[1]      // 득표 수
                ))
                .toList();

        log.info("카테고리별 득표 현황 조회 완료 - 총 {}개 협업툴", tools.size());

        return new ToolVoteCountResponse(categoryId, tools);
    }

    /**
     * 투표 참여 현황 조회
     * - 일괄 제출 완료한 멤버와 미완료 멤버 분류
     * - User 정보 포함 (Fetch Join으로 N+1 방지)
     */
    public ToolVoteParticipationResponse getVotedMemberParticipation(Long teamRoomId, Long userId) {
        log.info("투표 참여 현황 조회 시작 - teamRoomId: {}", teamRoomId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 팀원인지 확인
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 전체 팀원 조회
        List<TeamMember> allMembers = teamMemberRepository.findByTeamRoomId(teamRoomId);

        // 4. 투표 완료한 멤버 ID 목록 조회
        List<Long> votedMemberIds = toolVoteRepository.findVotedMemberIds(teamRoomId);
        Set<Long> votedMemberIdSet = new HashSet<>(votedMemberIds);

        // 5. 투표 완료/미완료 분류
        List<ToolVoteParticipationResponse.MemberInfo> voted = new ArrayList<>();
        List<ToolVoteParticipationResponse.MemberInfo> notVoted = new ArrayList<>();

        for (TeamMember member : allMembers) {
            User user = member.getUser();
            ToolVoteParticipationResponse.MemberInfo memberInfo =
                    new ToolVoteParticipationResponse.MemberInfo(
                            user.getId(),
                            user.getName(),
                            user.getProfileImageId()
                    );

            if (votedMemberIdSet.contains(member.getId())) {
                voted.add(memberInfo);
            } else {
                notVoted.add(memberInfo);
            }
        }

        log.info("투표 참여 현황 조회 완료 - 전체: {}, 투표완료: {}", allMembers.size(), voted.size());

        return new ToolVoteParticipationResponse(
                allMembers.size(),
                voted.size(),
                voted,
                notVoted
        );
    }

    /**
     * 협업툴 확정 처리 (내부 메서드)
     * - 카테고리별 최다 득표 Top3 확정
     * - 동률 시 모두 확정
     */
    private void confirmTools(Long teamRoomId) {
        log.info("협업툴 확정 처리 시작 - teamRoomId: {}", teamRoomId);

        // 1. 모든 투표 조회
        List<MemberToolVote> allVotes = toolVoteRepository.findByTeamRoomId(teamRoomId);

        // 2. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 3. 카테고리별로 그룹화
        Map<Integer, List<MemberToolVote>> votesByCategory = allVotes.stream()
                .collect(Collectors.groupingBy(MemberToolVote::getCategoryId));

        // 4. 각 카테고리별로 Top3 확정 처리
        for (Map.Entry<Integer, List<MemberToolVote>> entry : votesByCategory.entrySet()) {
            Integer categoryId = entry.getKey();
            List<MemberToolVote> categoryVotes = entry.getValue();

            // 툴별 득표 수 집계
            Map<Integer, Long> voteCountByTool = categoryVotes.stream()
                    .collect(Collectors.groupingBy(
                            MemberToolVote::getToolId,
                            Collectors.counting()
                    ));

            // 득표 수 기준 내림차순 정렬 후 Top3 추출 (동률 포함)
            List<Map.Entry<Integer, Long>> sortedVotes = voteCountByTool.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .toList();

            // Top3의 득표수 가져오기
            Set<Long> top3Votes = sortedVotes.stream()
                    .limit(3)
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet());

            // Top3 득표수에 해당하는 모든 툴 확정 (동률 포함)
            List<Integer> confirmedToolIds = voteCountByTool.entrySet().stream()
                    .filter(e -> top3Votes.contains(e.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();

            // 확정 툴 저장
            for (Integer toolId : confirmedToolIds) {
                TeamTool teamTool = TeamTool.create(teamRoom, categoryId, toolId);
                teamToolRepository.save(teamTool);

                log.info("협업툴 확정 - categoryId: {}, toolId: {}", categoryId, toolId);
            }
        }

        log.info("협업툴 확정 완료 - teamRoomId: {}", teamRoomId);
    }

    /**
     * 확정된 협업툴 조회
     */
    public ConfirmedToolsResponse getConfirmedTools(Long teamRoomId, Long userId) {
        log.info("확정된 협업툴 조회 시작 - teamRoomId: {}, userId: {}", teamRoomId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 요청자가 팀원인지 확인
        teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        // 3. 확정된 협업툴 조회
        List<TeamTool> teamTools = teamToolRepository.findByTeamRoomId(teamRoomId);

        // 4. 카테고리별로 그룹화
        Map<Integer, List<Integer>> toolsByCategory = teamTools.stream()
                .collect(Collectors.groupingBy(
                        TeamTool::getCategoryId,
                        Collectors.mapping(TeamTool::getToolId, Collectors.toList())
                ));

        // 5. DTO 변환
        List<ConfirmedToolsResponse.CategoryTools> confirmedTools = toolsByCategory.entrySet().stream()
                .map(entry -> new ConfirmedToolsResponse.CategoryTools(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();

        log.info("확정된 협업툴 조회 완료 - 총 {}개 카테고리", confirmedTools.size());

        return new ConfirmedToolsResponse(confirmedTools);
    }

    /**
     * 협업툴 삭제 (팀장만)
     */
    @Transactional
    public void deleteTeamTool(Long teamRoomId, Long teamToolId, Long userId) {
        log.info("협업툴 삭제 시작 - teamRoomId: {}, teamToolId: {}, userId: {}",
                teamRoomId, teamToolId, userId);

        // 1. 팀룸 조회
        TeamRoom teamRoom = teamRoomRepository.findById(teamRoomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        // 2. 팀장 권한 확인
        TeamMember member = teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        if (!member.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_TEAM_MANAGER);
        }

        // 3. 협업툴이 해당 팀룸에 속하는지 확인
        if (!teamToolRepository.existsByIdAndTeamRoomId(teamToolId, teamRoomId)) {
            throw new YamoyoException(ErrorCode.TOOL_NOT_FOUND);
        }

        // 4. 협업툴 삭제
        teamToolRepository.deleteById(teamToolId);

        log.info("협업툴 삭제 완료 - teamToolId: {}", teamToolId);
    }
}
