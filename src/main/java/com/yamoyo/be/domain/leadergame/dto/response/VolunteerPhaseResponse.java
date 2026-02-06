package com.yamoyo.be.domain.leadergame.dto.response;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "지원 단계 응답")
public record VolunteerPhaseResponse(
        @Schema(description = "현재 게임 단계", example = "VOLUNTEER")
        GamePhase phase,

        @Schema(description = "단계 시작 시간 (Unix timestamp)", example = "1704067200000")
        Long phaseStartTime,

        @Schema(description = "단계 지속 시간 (초)", example = "10")
        Long durationSeconds,

        @Schema(description = "참가자 목록")
        List<GameParticipant> participants
) {
    public static VolunteerPhaseResponse of(Long phaseStartTime, Long durationSeconds, List<GameParticipant> participants) {
        return new VolunteerPhaseResponse(GamePhase.VOLUNTEER, phaseStartTime, durationSeconds, participants);
    }
}
