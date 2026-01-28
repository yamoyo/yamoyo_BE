package com.yamoyo.be.domain.collabtool.service;

import com.yamoyo.be.domain.collabtool.dto.request.ToolVoteRequest;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteCountResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteParticipationResponse;
import com.yamoyo.be.domain.collabtool.entity.MemberToolVote;
import com.yamoyo.be.domain.collabtool.repository.MemberToolVoteRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ToolService {

    private final MemberToolVoteRepository toolVoteRepository;
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
}
