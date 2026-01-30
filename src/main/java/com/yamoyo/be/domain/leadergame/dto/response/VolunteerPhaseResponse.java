package com.yamoyo.be.domain.leadergame.dto.response;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.enums.GamePhase;

import java.util.List;

public record VolunteerPhaseResponse(
        GamePhase phase,
        Long phaseStartTime,
        Long durationSeconds,
        List<GameParticipant> participants
) {
    public static VolunteerPhaseResponse of(Long phaseStartTime, Long durationSeconds, List<GameParticipant> participants) {
        return new VolunteerPhaseResponse(GamePhase.VOLUNTEER, phaseStartTime, durationSeconds, participants);
    }
}
