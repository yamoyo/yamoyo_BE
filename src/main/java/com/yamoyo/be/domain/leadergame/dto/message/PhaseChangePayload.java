package com.yamoyo.be.domain.leadergame.dto.message;

import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PhaseChangePayload {
    private GamePhase phase;
    private Long phaseStartTime;
    private Long durationSeconds;
    private GameType selectedGame;

    public static PhaseChangePayload of(GamePhase phase, Long phaseStartTime, Long durationSeconds) {
        return new PhaseChangePayload(phase, phaseStartTime, durationSeconds, null);
    }

    public static PhaseChangePayload of(GamePhase phase, Long phaseStartTime, Long durationSeconds, GameType selectedGame) {
        return new PhaseChangePayload(phase, phaseStartTime, durationSeconds, selectedGame);
    }
}