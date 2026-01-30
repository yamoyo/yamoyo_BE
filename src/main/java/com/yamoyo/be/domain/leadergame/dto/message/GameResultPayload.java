package com.yamoyo.be.domain.leadergame.dto.message;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameResultPayload {
    private GameType gameType;
    private Long winnerId;
    private String winnerName;
    private List<GameParticipant> participants;
    private Object gameData;

    public static GameResultPayload ladder(Long winnerId, String winnerName,
                                            List<GameParticipant> participants,
                                            Object ladderData) {
        return new GameResultPayload(GameType.LADDER, winnerId, winnerName, participants, ladderData);
    }

    public static GameResultPayload roulette(Long winnerId, String winnerName,
                                              List<GameParticipant> participants,
                                              int winnerIndex) {
        return new GameResultPayload(GameType.ROULETTE, winnerId, winnerName, participants, winnerIndex);
    }

    public static GameResultPayload timing(Long winnerId, String winnerName,
                                            List<GameParticipant> participants,
                                            Map<Long, Double> timingRecords) {
        return new GameResultPayload(GameType.TIMING, winnerId, winnerName, participants, timingRecords);
    }
}