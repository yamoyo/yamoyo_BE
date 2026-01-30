package com.yamoyo.be.domain.leadergame.dto;

import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private GamePhase phase;
    private Long phaseStartTime;
    private Set<Long> volunteers;
    private GameType selectedGame;
    private Long gameStartTime;
    private List<GameParticipant> participants;
    private Map<Long, Double> timingRecords;
    private Long winnerId;
    private Object gameResult;
}
