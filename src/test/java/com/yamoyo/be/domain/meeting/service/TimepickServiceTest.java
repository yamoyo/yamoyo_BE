package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.entity.Timepick;
import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickParticipantStatus;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickStatus;
import com.yamoyo.be.domain.meeting.repository.TimepickParticipantRepository;
import com.yamoyo.be.domain.meeting.repository.TimepickRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimepickService 단위 테스트")
class TimepickServiceTest {

    @Mock
    private TimepickRepository timepickRepository;

    @Mock
    private TimepickParticipantRepository timepickParticipantRepository;

    @InjectMocks
    private TimepickService timepickService;

    private static final Long TEAM_ROOM_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long TIMEPICK_ID = 1L;

    @Nested
    @DisplayName("getTimepick() - 타임픽 조회")
    class GetTimepickTest {

        @Test
        @DisplayName("정상 조회 - 타임픽과 참가자 정보를 반환한다")
        void getTimepick_Success() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            TimepickParticipant participant = createParticipant(
                    TimepickParticipantStatus.PENDING,
                    TimepickParticipantStatus.PENDING
            );

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when
            TimepickResponse response = timepickService.getTimepick(TEAM_ROOM_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.timepickId()).isEqualTo(TIMEPICK_ID);
            assertThat(response.status()).isEqualTo(TimepickStatus.OPEN);
            assertThat(response.deadline()).isNotNull();
            assertThat(response.myStatus().availabilityStatus()).isEqualTo(TimepickParticipantStatus.PENDING);
            assertThat(response.myStatus().preferredBlockStatus()).isEqualTo(TimepickParticipantStatus.PENDING);
        }

        @Test
        @DisplayName("타임픽이 존재하지 않으면 TIMEPICK_NOT_FOUND 예외 발생")
        void getTimepick_TimepickNotFound() {
            // given
            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timepickService.getTimepick(TEAM_ROOM_ID, USER_ID))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("참가자가 아니면 TIMEPICK_NOT_PARTICIPANT 예외 발생")
        void getTimepick_NotParticipant() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timepickService.getTimepick(TEAM_ROOM_ID, USER_ID))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_PARTICIPANT);
                    });
        }

        @Test
        @DisplayName("둘 다 SUBMITTED 상태인 경우 정상 조회")
        void getTimepick_BothSubmitted() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            TimepickParticipant participant = createParticipant(
                    TimepickParticipantStatus.SUBMITTED,
                    TimepickParticipantStatus.SUBMITTED
            );

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when
            TimepickResponse response = timepickService.getTimepick(TEAM_ROOM_ID, USER_ID);

            // then
            assertThat(response.myStatus().availabilityStatus()).isEqualTo(TimepickParticipantStatus.SUBMITTED);
            assertThat(response.myStatus().preferredBlockStatus()).isEqualTo(TimepickParticipantStatus.SUBMITTED);
        }

        @Test
        @DisplayName("FINALIZED 상태 조회")
        void getTimepick_FinalizedStatus() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.FINALIZED);
            TimepickParticipant participant = createParticipant(
                    TimepickParticipantStatus.SUBMITTED,
                    TimepickParticipantStatus.SUBMITTED
            );

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when
            TimepickResponse response = timepickService.getTimepick(TEAM_ROOM_ID, USER_ID);

            // then
            assertThat(response.status()).isEqualTo(TimepickStatus.FINALIZED);
        }
    }

    // ========== Helper Methods ==========

    private Timepick createTimepick(TimepickStatus status) {
        try {
            var constructor = Timepick.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Timepick timepick = constructor.newInstance();
            ReflectionTestUtils.setField(timepick, "id", TIMEPICK_ID);
            ReflectionTestUtils.setField(timepick, "status", status);
            ReflectionTestUtils.setField(timepick, "deadline", LocalDateTime.now().plusDays(7));
            return timepick;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TimepickParticipant createParticipant(
            TimepickParticipantStatus availabilityStatus,
            TimepickParticipantStatus preferredBlockStatus
    ) {
        try {
            var constructor = TimepickParticipant.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            TimepickParticipant participant = constructor.newInstance();
            ReflectionTestUtils.setField(participant, "id", 1L);
            ReflectionTestUtils.setField(participant, "availabilityStatus", availabilityStatus);
            ReflectionTestUtils.setField(participant, "preferredBlockStatus", preferredBlockStatus);
            return participant;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
