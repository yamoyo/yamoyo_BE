package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.message.GameResultPayload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class RouletteGameService {

    public GameResultPayload play(List<GameParticipant> participants) {
        int count = participants.size();

        // 랜덤으로 당첨자(팀장) 선정
        Random random = new Random();
        int winnerIndex = random.nextInt(count);
        GameParticipant winner = participants.get(winnerIndex);

        return GameResultPayload.roulette(
                winner.userId(),
                winner.name(),
                participants,
                winnerIndex
        );
    }
}