package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.message.*;
import com.yamoyo.be.domain.leadergame.dto.response.VolunteerPhaseResponse;
import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import com.yamoyo.be.domain.notification.entity.NotificationType;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.event.event.NotificationEvent;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderGameService {

    private final GameStateRedisService redisService;
    private final LeaderGameDbService dbService;
    private final LadderGameService ladderGameService;
    private final RouletteGameService rouletteGameService;
    private final TeamRoomRepository teamRoomRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    private static final long VOLUNTEER_DURATION_SECONDS = 60000;

    private String getTopic(Long roomId) {
        return "/sub/room/" + roomId;
    }

    // ========================================================================
    // 1. 입장 / 퇴장
    // ========================================================================

    public JoinPayload handleJoin(Long roomId, User user) {
        TeamRoom teamRoom = teamRoomRepository.findById(roomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        Workflow workflow = teamRoom.getWorkflow();
        if (workflow != Workflow.PENDING) {
            throw new YamoyoException(ErrorCode.ROOM_NOT_JOINABLE);
        }

        teamMemberRepository.findByTeamRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        redisService.addConnection(roomId, user.getId());

        GameParticipant participant = GameParticipant.of(
                user.getId(), user.getName(),
                user.getProfileImageId() != null ? user.getProfileImageId().toString() : null
        );

        List<GameParticipant> participants = redisService.getParticipants(roomId);
        if (participants.isEmpty()) {
            participants = getParticipantsFromDb(roomId);
        }

        Set<Long> connectedUserIds = redisService.getConnectedUsers(roomId);
        GamePhase currentPhase = redisService.getPhase(roomId);
        Long phaseStartTime = redisService.getPhaseStartTime(roomId);

        Long remainingTime = null;
        if (currentPhase == GamePhase.VOLUNTEER && phaseStartTime != null) {
            long elapsed = (System.currentTimeMillis() - phaseStartTime) / 1000;
            remainingTime = Math.max(0, VOLUNTEER_DURATION_SECONDS - elapsed);
        }

        broadcast(roomId, "USER_JOINED", participant);

        return JoinPayload.of(participant, participants, connectedUserIds,
                currentPhase, phaseStartTime, remainingTime);
    }

    public void handleLeave(Long roomId, Long userId) {
        redisService.removeConnection(roomId, userId);
        broadcast(roomId, "USER_LEFT", Map.of("userId", userId));
    }

    // ========================================================================
    // 2. 게임 시작 → VOLUNTEER (10초)
    // ========================================================================

    /**
     * HTTP 방식: 지원 단계 시작 (방장 전용)
     * - 전원 온라인 확인
     * - Redis 게임 초기화
     * - WebSocket으로 PHASE_CHANGE 브로드캐스트
     * - 10초 후 endVolunteerPhase 스케줄링
     *
     * @return 지원 단계 정보 (HTTP 응답용)
     */
    public VolunteerPhaseResponse startVolunteerPhase(Long roomId, Long userId) {
        TeamRoom teamRoom = teamRoomRepository.findById(roomId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND));

        if (teamRoom.getWorkflow() != Workflow.PENDING) {
            throw new YamoyoException(ErrorCode.GAME_ALREADY_IN_PROGRESS);
        }

        TeamMember host = teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!host.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_ROOM_HOST);
        }

        if (redisService.isGameExists(roomId)) {
            throw new YamoyoException(ErrorCode.GAME_ALREADY_IN_PROGRESS);
        }

        // 전원 온라인 확인
        List<TeamMember> members = teamMemberRepository.findByTeamRoomId(roomId);
        List<Long> memberIds = members.stream().map(m -> m.getUser().getId()).toList();
        if (!redisService.areAllMembersConnected(roomId, memberIds)) {
            throw new YamoyoException(ErrorCode.NOT_ALL_MEMBERS_CONNECTED);
        }

        // DB 상태 변경
        dbService.startLeaderSelection(roomId);

        // Redis 게임 초기화
        List<GameParticipant> participants = getParticipantsFromDb(roomId);
        redisService.initializeGame(roomId, participants);

        long phaseStartTime = System.currentTimeMillis();

        // WebSocket 브로드캐스트
        PhaseChangePayload payload = PhaseChangePayload.of(
                GamePhase.VOLUNTEER, phaseStartTime, VOLUNTEER_DURATION_SECONDS);
        broadcast(roomId, "PHASE_CHANGE", payload);

        // 10초 후 지원 단계 종료
        taskScheduler.schedule(
                () -> endVolunteerPhase(roomId),
                Instant.now().plusSeconds(VOLUNTEER_DURATION_SECONDS)
        );

        return VolunteerPhaseResponse.of(phaseStartTime, VOLUNTEER_DURATION_SECONDS, participants);
    }

    // ========================================================================
    // 3. 팀장 지원
    // ========================================================================

    public void vote(Long roomId, Long userId, boolean isVolunteer) {
        GamePhase phase = redisService.getPhase(roomId);
        if (phase != GamePhase.VOLUNTEER) {
            throw new YamoyoException(ErrorCode.INVALID_GAME_PHASE);
        }

        teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));

        if (redisService.hasVoted(roomId, userId)) {
            throw new YamoyoException(ErrorCode.ALREADY_VOLUNTEER);
        }

        // 투표 완료 기록
        redisService.addVotedUser(roomId, userId);

        // 지원하기인 경우 지원자 목록에 추가
        if (isVolunteer) {
            redisService.addVolunteer(roomId, userId);
        }

        List<GameParticipant> participants = redisService.getParticipants(roomId);

        Set<Long> votedUsers = redisService.getVotedUsers(roomId);
        if(votedUsers.size() == participants.size()) {
            endVolunteerPhase(roomId);
        }

        // 투표 현황 브로드캐스트
        broadcastVoteStatus(roomId);
    }

    /**
     * 투표 현황을 투표 완료자에게만 전송
     * - 프론트엔드 구독 경로: /user/queue/vote-status
     * - convertAndSendToUser 사용으로 사용자별 전송
     */
    private void broadcastVoteStatus(Long roomId) {
        // 게임 참가자 목록 ID
        Set<Long> participantsIds = redisService.getParticipants(roomId).stream()
                .map(GameParticipant::userId)
                .collect(Collectors.toSet());

        // redis에 저장된 투표 완료자
        Set<Long> votedUserIds = redisService.getVotedUsers(roomId);

        // 게임 참가자 목록 ID 중 투표 완료자에 포함되지 않는 자들 (미투표자)
        Set<Long> unvotedUserIds = participantsIds.stream()
                .filter(userId -> !votedUserIds.contains(userId))
                .collect(Collectors.toSet());

        // redis에 저장된 지원자
        Set<Long> volunteerIds = redisService.getVolunteers(roomId);
        List<GameParticipant> participants = redisService.getParticipants(roomId);

        VoteStatusPayload payload = VoteStatusPayload.of(
                votedUserIds, unvotedUserIds, volunteerIds, participants.size());
        GameMessage<VoteStatusPayload> message = GameMessage.of("VOTE_UPDATED", payload);

        // 투표 완료자에게만 전송
        for (Long votedUserId : votedUserIds) {
            messagingTemplate.convertAndSendToUser(
                    votedUserId.toString(),
                    "/queue/vote-status",
                    message
            );
        }
    }

    // ========================================================================
    // 4. 게임 선택
    //    - LADDER / ROULETTE → 즉시 결과 계산 + GAME_RESULT 전송
    //    - TIMING → GAME_READY (방장의 시작 버튼 대기)
    // ========================================================================

    public void selectGame(Long roomId, Long userId, GameType gameType) {
        GamePhase phase = redisService.getPhase(roomId);
        if (phase != GamePhase.GAME_SELECT) {
            throw new YamoyoException(ErrorCode.INVALID_GAME_PHASE);
        }

        TeamMember host = teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!host.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_ROOM_HOST);
        }

        redisService.setSelectedGame(roomId, gameType);
        List<GameParticipant> candidates = filterVolunteersOnly(roomId);

        switch (gameType) {
            case LADDER -> {
                GameResultPayload result = ladderGameService.play(candidates);
                finishGame(roomId, result.getWinnerId());
                broadcast(roomId, "GAME_RESULT", result);
            }
            case ROULETTE -> {
                GameResultPayload result = rouletteGameService.play(candidates);
                finishGame(roomId, result.getWinnerId());
                broadcast(roomId, "GAME_RESULT", result);
            }
            case TIMING -> {
                redisService.setPhase(roomId, GamePhase.GAME_READY);
                broadcast(roomId, "PHASE_CHANGE",
                        PhaseChangePayload.of(GamePhase.GAME_READY, System.currentTimeMillis(), null, GameType.TIMING));
            }
        }
    }

    // ========================================================================
    // 5. 타이밍 게임 시작 (방장 클릭) → GAME_PLAYING 전환
    //    - 각 유저는 개별적으로 시작/정지
    //    - 시간 제어는 프론트엔드에서 담당
    // ========================================================================

    public void startTimingGame(Long roomId, Long userId) {
        GamePhase phase = redisService.getPhase(roomId);
        if (phase != GamePhase.GAME_READY) {
            throw new YamoyoException(ErrorCode.INVALID_GAME_PHASE);
        }

        TeamMember host = teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.NOT_TEAM_MEMBER));
        if (!host.hasManagementAuthority()) {
            throw new YamoyoException(ErrorCode.NOT_ROOM_HOST);
        }

        redisService.setPhase(roomId, GamePhase.GAME_PLAYING);

        // 각 유저가 개별적으로 시작/정지하므로 서버에서 타이머 스케줄링 없음
        broadcast(roomId, "PHASE_CHANGE",
                PhaseChangePayload.of(GamePhase.GAME_PLAYING, System.currentTimeMillis(), null, GameType.TIMING));
    }

    // ========================================================================
    // 6. 타이밍 게임 결과 제출 (각 유저)
    //    - 프론트에서 |7.777 - 실제시간| 계산하여 전송
    //    - Redis SortedSet에 저장 (score = timeDifference)
    //    - 전원 제출 시 결과 계산
    // ========================================================================

    public void submitTimingResult(Long roomId, Long userId, Double timeDifference) {
        GamePhase phase = redisService.getPhase(roomId);
        if (phase != GamePhase.GAME_PLAYING) {
            throw new YamoyoException(ErrorCode.INVALID_GAME_PHASE);
        }
        if (redisService.getSelectedGame(roomId) != GameType.TIMING) {
            throw new YamoyoException(ErrorCode.INVALID_GAME_PHASE);
        }

        // 중복 제출 방지
        if (redisService.hasTimingRecord(roomId, userId)) {
            throw new YamoyoException(ErrorCode.TIMING_ALREADY_STOPPED);
        }

        // Redis SortedSet에 기록 저장
        redisService.recordTimingResult(roomId, userId, timeDifference);

        // 지원자 전원이 제출 완료하면 결과 계산
        List<GameParticipant> candidates = filterVolunteersOnly(roomId);
        long recordCount = redisService.getTimingRecordCount(roomId);
        if (recordCount >= candidates.size()) {
            finishTimingGame(roomId);
        }
    }

    // ========================================================================
    // 7. 결과 확인 → 게임 데이터 정리
    // ========================================================================

    public void confirmResult(Long roomId, Long userId) {
        redisService.removeConnection(roomId, userId);
        if (redisService.getConnectedUsers(roomId).isEmpty()) {
            redisService.deleteGame(roomId);
            redisService.deleteTimingRecords(roomId);  // 타이밍 기록도 정리
        }
    }

    // ========================================================================
    // Private
    // ========================================================================

    private void endVolunteerPhase(Long roomId) {
        if (redisService.getPhase(roomId) != GamePhase.VOLUNTEER) {
            return;
        }

        Set<Long> volunteers = redisService.getVolunteers(roomId);
        List<GameParticipant> participants = redisService.getParticipants(roomId);

        // 지원자 없다면 전원 지원자로 추가
        if (volunteers.isEmpty()) {
            volunteers = participants.stream()
                    .map(GameParticipant::userId)
                    .collect(Collectors.toSet());
            for (Long uid : volunteers) {
                redisService.addVolunteer(roomId, uid);
            }
        }

        // 지원자 1명일 때
        if (volunteers.size() == 1) {
            Long winnerId = volunteers.iterator().next();
            finishGame(roomId, winnerId);

            String winnerName = participants.stream()
                    .filter(p -> p.userId().equals(winnerId))
                    .findFirst().map(GameParticipant::name).orElse("Unknown");

            broadcast(roomId, "GAME_RESULT",
                    new GameResultPayload(GameType.LADDER, winnerId, winnerName, participants, null));
        } else {
            redisService.setPhase(roomId, GamePhase.GAME_SELECT);
            broadcast(roomId, "PHASE_CHANGE",
                    PhaseChangePayload.of(GamePhase.GAME_SELECT, System.currentTimeMillis(), null));
        }
    }

    private void finishTimingGame(Long roomId) {
        if (redisService.getPhase(roomId) != GamePhase.GAME_PLAYING) {
            return;
        }

        // SortedSet에서 차이가 가장 큰 유저 (index 0) 조회
        Long winnerId = redisService.getMaxDifferenceUserId(roomId);
        List<GameParticipant> candidates = filterVolunteersOnly(roomId);

        String winnerName = candidates.stream()
                .filter(p -> p.userId().equals(winnerId))
                .findFirst()
                .map(GameParticipant::name)
                .orElse("Unknown");

        finishGame(roomId, winnerId);
        broadcast(roomId, "GAME_RESULT",
                GameResultPayload.timing(winnerId, winnerName, candidates, null));
    }

    private void finishGame(Long roomId, Long winnerId) {
        redisService.setPhase(roomId, GamePhase.RESULT);
        redisService.setWinner(roomId, winnerId);
        dbService.applyLeaderResult(roomId, winnerId);

        eventPublisher.publishEvent(NotificationEvent.ofSingle(
                roomId,
                winnerId,
                NotificationType.TEAM_LEADER_CONFIRM
        ));
    }

    private List<GameParticipant> filterVolunteersOnly(Long roomId) {
        Set<Long> volunteerIds = redisService.getVolunteers(roomId);
        List<GameParticipant> all = redisService.getParticipants(roomId);
        if (volunteerIds.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(p -> volunteerIds.contains(p.userId()))
                .collect(Collectors.toList());
    }

    private List<GameParticipant> getParticipantsFromDb(Long roomId) {
        return teamMemberRepository.findByTeamRoomId(roomId).stream()
                .map(m -> GameParticipant.of(
                        m.getUser().getId(), m.getUser().getName(),
                        m.getUser().getProfileImageId() != null ?
                                m.getUser().getProfileImageId().toString() : null))
                .collect(Collectors.toList());
    }

    private <T> void broadcast(Long roomId, String type, T payload) {
        messagingTemplate.convertAndSend(getTopic(roomId), GameMessage.of(type, payload));
    }
}
