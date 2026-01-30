package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.message.GameResultPayload;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LadderGameService 테스트")
class LadderGameServiceTest {

    private final LadderGameService ladderGameService = new LadderGameService();

    private List<GameParticipant> createParticipants(int count) {
        List<GameParticipant> participants = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            participants.add(GameParticipant.of((long) i, "User" + i, String.valueOf(i)));
        }
        return participants;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGameDataAsMap(GameResultPayload result) {
        return (Map<String, Object>) result.getGameData();
    }

    @Nested
    @DisplayName("play - 사다리 게임 실행")
    class PlayTest {

        @Test
        @DisplayName("2명 참가자 사다리 게임 성공")
        void play_TwoParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(2);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getGameType()).isEqualTo(GameType.LADDER);
            assertThat(result.getWinnerId()).isIn(1L, 2L);
            assertThat(result.getWinnerName()).isIn("User1", "User2");
            assertThat(result.getParticipants()).hasSize(2);
            assertThat(result.getGameData()).isNotNull();

            Map<String, Object> gameData = getGameDataAsMap(result);
            assertThat(gameData).containsKey("ladderLines");
            assertThat(gameData).containsKey("mappings");
            assertThat(gameData).containsKey("winnerStartIndex");
        }

        @Test
        @DisplayName("3명 참가자 사다리 게임 성공")
        void play_ThreeParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(3);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isIn(1L, 2L, 3L);
            assertThat(result.getParticipants()).hasSize(3);
        }

        @Test
        @DisplayName("5명 참가자 사다리 게임 성공")
        void play_FiveParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(5);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isBetween(1L, 5L);
            assertThat(result.getParticipants()).hasSize(5);
        }

        @Test
        @DisplayName("사다리 매핑은 1:1 대응이어야 함")
        @SuppressWarnings("unchecked")
        void play_MappingsAreBijective() {
            // given
            List<GameParticipant> participants = createParticipants(4);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            Map<String, Object> gameData = getGameDataAsMap(result);
            Map<Integer, Integer> mappings = (Map<Integer, Integer>) gameData.get("mappings");
            assertThat(mappings).hasSize(4);

            // 모든 시작점이 다른 도착점으로 매핑되어야 함 (1:1 대응)
            Set<Integer> destinations = new HashSet<>(mappings.values());
            assertThat(destinations).hasSize(4);
        }

        @Test
        @DisplayName("당첨자는 도착 위치 1번에 도달해야 함")
        @SuppressWarnings("unchecked")
        void play_WinnerReachesPositionOne() {
            // given
            List<GameParticipant> participants = createParticipants(3);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            Map<String, Object> gameData = getGameDataAsMap(result);
            Map<Integer, Integer> mappings = (Map<Integer, Integer>) gameData.get("mappings");
            Integer winnerStartIndex = (Integer) gameData.get("winnerStartIndex");

            // 당첨자의 시작 인덱스에서 도착한 위치가 1이어야 함
            assertThat(mappings.get(winnerStartIndex)).isEqualTo(1);
        }

        @RepeatedTest(10)
        @DisplayName("사다리 게임 랜덤성 확인 - 여러 번 실행해도 결과가 유효함")
        void play_RandomnessValidation() {
            // given
            List<GameParticipant> participants = createParticipants(4);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isBetween(1L, 4L);
            Map<String, Object> gameData = getGameDataAsMap(result);
            assertThat(gameData.get("ladderLines")).isNotNull();
        }

        @Test
        @DisplayName("사다리 라인은 10개의 행으로 구성됨")
        @SuppressWarnings("unchecked")
        void play_LadderHasTenRows() {
            // given
            List<GameParticipant> participants = createParticipants(3);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            Map<String, Object> gameData = getGameDataAsMap(result);
            List<List<Integer>> ladderLines = (List<List<Integer>>) gameData.get("ladderLines");
            assertThat(ladderLines).hasSize(10);
        }

        @Test
        @DisplayName("각 행의 가로줄은 연속되지 않음")
        @SuppressWarnings("unchecked")
        void play_NoConsecutiveHorizontalLines() {
            // given
            List<GameParticipant> participants = createParticipants(5);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            Map<String, Object> gameData = getGameDataAsMap(result);
            List<List<Integer>> ladderLines = (List<List<Integer>>) gameData.get("ladderLines");

            for (List<Integer> row : ladderLines) {
                for (int i = 0; i < row.size() - 1; i++) {
                    // 연속된 가로줄이 있으면 안됨 (col, col+1 동시에 있으면 안됨)
                    int current = row.get(i);
                    int next = row.get(i + 1);
                    assertThat(next - current).isGreaterThan(1);
                }
            }
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCaseTest {

        @RepeatedTest(20)
        @DisplayName("2명일 때도 항상 유효한 결과 반환")
        void play_TwoParticipants_AlwaysValid() {
            // given
            List<GameParticipant> participants = createParticipants(2);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            assertThat(result.getWinnerId()).isIn(1L, 2L);
            assertThat(result.getWinnerName()).matches("User[12]");
        }

        @Test
        @DisplayName("많은 참가자(10명)도 정상 처리")
        void play_ManyParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(10);

            // when
            GameResultPayload result = ladderGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isBetween(1L, 10L);
            assertThat(result.getParticipants()).hasSize(10);
        }
    }
}
