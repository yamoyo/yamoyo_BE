package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.meeting.service.TimepickService;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaderGameDbService {

    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoomSetupRepository teamRoomSetupRepository;
    private final TimepickService timepickService;

    /**
     * 팀장 선출 결과를 DB에 반영
     * - 당첨자를 LEADER로 승격
     * - 기존 HOST를 MEMBER로 강등
     * - TeamRoom workflow를 SETUP으로 전환
     * - TeamRoomSetup 생성 (6시간 타이머 시작)
     */
    @Transactional
    public void applyLeaderResult(Long roomId, Long winnerId) {
        // 당첨자를 LEADER로 승격
        TeamMember winner = teamMemberRepository.findByTeamRoomIdAndUserId(roomId, winnerId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_MEMBER_NOT_FOUND));
        winner.promoteToLeader();

        // HOST를 MEMBER로 강등
        teamMemberRepository.findByTeamRoomIdAndTeamRole(roomId, TeamRole.HOST)
                .ifPresent(TeamMember::demoteToMember);

        // TeamRoom 상태 변경
        TeamRoom teamRoom = teamRoomRepository.findById(roomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));
        teamRoom.completeLeaderSelection();

        // TeamRoomSetup 생성 (6시간 타이머 시작)
        TeamRoomSetup setup = TeamRoomSetup.create(teamRoom);
        teamRoomSetupRepository.save(setup);

        // 타임픽 자동 생성
        timepickService.createTimepick(roomId);
    }

    /**
     * 게임 시작 시 TeamRoom workflow를 LEADER_SELECTION으로 전환
     */
    @Transactional
    public void startLeaderSelection(Long roomId) {
        TeamRoom teamRoom = teamRoomRepository.findById(roomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));
        teamRoom.startLeaderSelection();
    }
}
