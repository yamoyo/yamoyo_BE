package com.yamoyo.be.domain.leadergame.dto.message;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import com.yamoyo.be.domain.leadergame.enums.GameType;

import java.util.List;
import java.util.Set;

/**
 * 새로고침/재접속 시 현재 방 상태를 전달하는 Payload
 *
 * Phase별 필요 정보:
 * - PENDING: members, connectedUserIds
 * - VOLUNTEER: + currentPhase, phaseStartTime, remainingTime, volunteers, votedUsers
 * - GAME_SELECT: + volunteers
 * - GAME_PLAYING: + selectedGame, volunteers
 * - RESULT: + selectedGame, winnerId, winnerName
 */
public record ReloadPayload(
        // 현재 유저 정보
        GameParticipant currentUser,

        // 전체 팀 멤버 (항상 전체, 온라인 상태 표시용)
        List<GameParticipant> members,
        Set<Long> connectedUserIds,

        // 게임 참가자 (지원자들)
        Set<Long> volunteers,

        // 투표 현황 (VOLUNTEER 단계)
        Set<Long> votedUsers,

        // 게임 상태
        GamePhase currentPhase,
        Long phaseStartTime,
        Long remainingTime,

        // 게임 결과
        GameType selectedGame,
        Long winnerId,
        String winnerName
) {
    public static ReloadPayload of(
            GameParticipant currentUser,
            List<GameParticipant> members,
            Set<Long> connectedUserIds,
            Set<Long> volunteers,
            Set<Long> votedUsers,
            GamePhase currentPhase,
            Long phaseStartTime,
            Long remainingTime,
            GameType selectedGame,
            Long winnerId,
            String winnerName
    ) {
        return new ReloadPayload(
                currentUser, members, connectedUserIds,
                volunteers, votedUsers,
                currentPhase, phaseStartTime, remainingTime,
                selectedGame, winnerId, winnerName
        );
    }
}
