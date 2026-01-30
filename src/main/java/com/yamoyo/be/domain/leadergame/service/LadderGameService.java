package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.message.GameResultPayload;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LadderGameService {

    private static final int LADDER_ROWS = 10;

    public GameResultPayload play(List<GameParticipant> participants) {
        int count = participants.size();
        Random random = new Random();

        // 1. 사다리 가로줄 생성
        List<List<Integer>> ladderLines = generateLadderLines(count, random);

        // 2. 각 시작 위치에서 사다리 타기 결과 계산
        Map<Integer, Integer> mappings = calculateMappings(count, ladderLines);

        // 3. 도착 위치 1번에 도달한 참가자가 팀장
        int winnerStartIndex = -1;
        for (Map.Entry<Integer, Integer> entry : mappings.entrySet()) {
            if (entry.getValue() == 1) {
                winnerStartIndex = entry.getKey();
                break;
            }
        }

        GameParticipant winner = participants.get(winnerStartIndex);

        // 4. gameData 구성
        Map<String, Object> gameData = new LinkedHashMap<>();
        gameData.put("ladderLines", ladderLines);
        gameData.put("mappings", mappings);
        gameData.put("winnerStartIndex", winnerStartIndex);

        return GameResultPayload.ladder(
                winner.userId(),
                winner.name(),
                participants,
                gameData
        );
    }

    /**
     * 사다리 가로줄 생성
     * 각 row마다 인접한 column 사이에 가로줄을 랜덤 배치
     * 연속된 가로줄은 허용하지 않음 (ex: 0-1, 1-2 동시 X)
     */
    private List<List<Integer>> generateLadderLines(int columns, Random random) {
        List<List<Integer>> lines = new ArrayList<>();

        for (int row = 0; row < LADDER_ROWS; row++) {
            List<Integer> rowLines = new ArrayList<>();
            int col = 0;

            while (col < columns - 1) {
                // 40% 확률로 가로줄 생성
                if (random.nextDouble() < 0.4) {
                    rowLines.add(col);
                    col += 2; // 연속 가로줄 방지
                } else {
                    col += 1;
                }
            }
            lines.add(rowLines);
        }

        return lines;
    }

    /**
     * 사다리 타기 결과 계산
     * 각 시작 위치에서 내려가며 가로줄 만나면 이동
     */
    private Map<Integer, Integer> calculateMappings(int columns, List<List<Integer>> ladderLines) {
        Map<Integer, Integer> mappings = new HashMap<>();

        for (int startCol = 0; startCol < columns; startCol++) {
            int currentCol = startCol;

            for (List<Integer> rowLines : ladderLines) {
                // 현재 위치에서 왼쪽으로 가는 가로줄 확인 (currentCol-1 위치에 가로줄)
                if (currentCol > 0 && rowLines.contains(currentCol - 1)) {
                    currentCol--;
                }
                // 현재 위치에서 오른쪽으로 가는 가로줄 확인 (currentCol 위치에 가로줄)
                else if (rowLines.contains(currentCol)) {
                    currentCol++;
                }
            }

            mappings.put(startCol, currentCol);
        }

        return mappings;
    }
}
