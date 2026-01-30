package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.message.GameResultPayload;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RouletteGameService 테스트")
class RouletteGameServiceTest {

    private final RouletteGameService rouletteGameService = new RouletteGameService();

    private List<GameParticipant> createParticipants(int count) {
        List<GameParticipant> participants = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            participants.add(GameParticipant.of((long) i, "User" + i, String.valueOf(i)));
        }
        return participants;
    }

    @Nested
    @DisplayName("play - 룰렛 게임 실행")
    class PlayTest {

        @Test
        @DisplayName("2명 참가자 룰렛 게임 성공")
        void play_TwoParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(2);

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getGameType()).isEqualTo(GameType.ROULETTE);
            assertThat(result.getWinnerId()).isIn(1L, 2L);
            assertThat(result.getWinnerName()).isIn("User1", "User2");
            assertThat(result.getParticipants()).hasSize(2);
        }

        @Test
        @DisplayName("3명 참가자 룰렛 게임 성공")
        void play_ThreeParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(3);

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isIn(1L, 2L, 3L);
            assertThat(result.getParticipants()).hasSize(3);
        }

        @Test
        @DisplayName("5명 참가자 룰렛 게임 성공")
        void play_FiveParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(5);

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isBetween(1L, 5L);
            assertThat(result.getParticipants()).hasSize(5);
        }

        @Test
        @DisplayName("gameData에 winnerIndex 포함")
        void play_GameDataContainsWinnerIndex() {
            // given
            List<GameParticipant> participants = createParticipants(4);

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result.getGameData()).isNotNull();
            Integer winnerIndex = (Integer) result.getGameData();
            assertThat(winnerIndex).isBetween(0, 3);
        }

        @Test
        @DisplayName("winnerIndex와 winnerId 일치 검증")
        void play_WinnerIndexMatchesWinnerId() {
            // given
            List<GameParticipant> participants = createParticipants(4);

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            Integer winnerIndex = (Integer) result.getGameData();
            GameParticipant expectedWinner = participants.get(winnerIndex);

            assertThat(result.getWinnerId()).isEqualTo(expectedWinner.userId());
            assertThat(result.getWinnerName()).isEqualTo(expectedWinner.name());
        }

        @RepeatedTest(10)
        @DisplayName("룰렛 게임 랜덤성 확인 - 여러 번 실행해도 결과가 유효함")
        void play_RandomnessValidation() {
            // given
            List<GameParticipant> participants = createParticipants(4);

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isBetween(1L, 4L);
            Integer winnerIndex = (Integer) result.getGameData();
            assertThat(winnerIndex).isBetween(0, 3);
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
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result.getWinnerId()).isIn(1L, 2L);
            Integer winnerIndex = (Integer) result.getGameData();
            assertThat(winnerIndex).isIn(0, 1);
        }

        @Test
        @DisplayName("많은 참가자(10명)도 정상 처리")
        void play_ManyParticipants_Success() {
            // given
            List<GameParticipant> participants = createParticipants(10);

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getWinnerId()).isBetween(1L, 10L);
            assertThat(result.getParticipants()).hasSize(10);
            Integer winnerIndex = (Integer) result.getGameData();
            assertThat(winnerIndex).isBetween(0, 9);
        }

        @Test
        @DisplayName("참가자 정보가 결과에 그대로 포함됨")
        void play_ParticipantsPreserved() {
            // given
            List<GameParticipant> participants = List.of(
                    GameParticipant.of(100L, "Alice", "profile1"),
                    GameParticipant.of(200L, "Bob", "profile2"),
                    GameParticipant.of(300L, "Charlie", "profile3")
            );

            // when
            GameResultPayload result = rouletteGameService.play(participants);

            // then
            assertThat(result.getParticipants()).hasSize(3);
            assertThat(result.getParticipants()).containsExactlyInAnyOrderElementsOf(participants);
            assertThat(result.getWinnerId()).isIn(100L, 200L, 300L);
        }
    }
}
