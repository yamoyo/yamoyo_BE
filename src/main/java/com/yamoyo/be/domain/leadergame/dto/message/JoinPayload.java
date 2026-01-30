package com.yamoyo.be.domain.leadergame.dto.message;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JoinPayload {
    private GameParticipant user;
    private List<GameParticipant> participants;
    private Set<Long> connectedUserIds;
    private GamePhase currentPhase;
    private Long phaseStartTime;
    private Long remainingTime;

    public static JoinPayload of(GameParticipant user, List<GameParticipant> participants,
                                  Set<Long> connectedUserIds, GamePhase currentPhase,
                                  Long phaseStartTime, Long remainingTime) {
        return new JoinPayload(user, participants, connectedUserIds, currentPhase, phaseStartTime, remainingTime);
    }
}