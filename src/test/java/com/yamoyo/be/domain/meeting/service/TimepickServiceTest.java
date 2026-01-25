package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.request.AvailabilitySubmitRequest;
import com.yamoyo.be.domain.meeting.dto.request.PreferredBlockSubmitRequest;
import com.yamoyo.be.domain.meeting.dto.response.TimepickResponse;
import com.yamoyo.be.domain.meeting.entity.Timepick;
import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimepickService 단위 테스트")
class TimepickServiceTest {

    @Mock
    private TimepickRepository timepickRepository;

    @Mock
    private TimepickParticipantRepository timepickParticipantRepository;

    @Mock
    private UserTimepickDefaultService userTimepickDefaultService;

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

    @Nested
    @DisplayName("submitAvailability() - 가용시간 제출")
    class SubmitAvailabilityTest {

        @Test
        @DisplayName("정상 제출 - 가용시간이 저장되고 기본값이 업데이트된다")
        void submitAvailability_Success() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            TimepickParticipant participant = createParticipant(
                    TimepickParticipantStatus.PENDING,
                    TimepickParticipantStatus.PENDING
            );
            AvailabilitySubmitRequest request = createAvailabilitySubmitRequest();

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when
            timepickService.submitAvailability(TEAM_ROOM_ID, USER_ID, request);

            // then
            assertThat(participant.getAvailabilityStatus()).isEqualTo(TimepickParticipantStatus.SUBMITTED);
            verify(userTimepickDefaultService).updateAvailability(eq(USER_ID), any());
        }

        @Test
        @DisplayName("타임픽이 존재하지 않으면 TIMEPICK_NOT_FOUND 예외 발생")
        void submitAvailability_TimepickNotFound() {
            // given
            AvailabilitySubmitRequest request = createAvailabilitySubmitRequest();

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timepickService.submitAvailability(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("타임픽이 FINALIZED 상태면 TIMEPICK_NOT_OPEN 예외 발생")
        void submitAvailability_TimepickFinalized() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.FINALIZED);
            AvailabilitySubmitRequest request = createAvailabilitySubmitRequest();

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));

            // when & then
            assertThatThrownBy(() -> timepickService.submitAvailability(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_OPEN);
                    });
        }

        @Test
        @DisplayName("참가자가 아니면 TIMEPICK_NOT_PARTICIPANT 예외 발생")
        void submitAvailability_NotParticipant() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            AvailabilitySubmitRequest request = createAvailabilitySubmitRequest();

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timepickService.submitAvailability(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_PARTICIPANT);
                    });
        }

        @Test
        @DisplayName("이미 제출한 경우 TIMEPICK_AVAILABILITY_ALREADY_SUBMITTED 예외 발생")
        void submitAvailability_AlreadySubmitted() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            TimepickParticipant participant = createParticipant(
                    TimepickParticipantStatus.SUBMITTED,
                    TimepickParticipantStatus.PENDING
            );
            AvailabilitySubmitRequest request = createAvailabilitySubmitRequest();

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when & then
            assertThatThrownBy(() -> timepickService.submitAvailability(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_AVAILABILITY_ALREADY_SUBMITTED);
                    });
        }
    }

    @Nested
    @DisplayName("submitPreferredBlock() - 선호시간대 제출")
    class SubmitPreferredBlockTest {

        @Test
        @DisplayName("정상 제출 - 선호시간대가 저장되고 기본값이 업데이트된다")
        void submitPreferredBlock_Success() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            TimepickParticipant participant = createParticipant(
                    TimepickParticipantStatus.PENDING,
                    TimepickParticipantStatus.PENDING
            );
            PreferredBlockSubmitRequest request = createPreferredBlockSubmitRequest(PreferredBlock.BLOCK_12_16);

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when
            timepickService.submitPreferredBlock(TEAM_ROOM_ID, USER_ID, request);

            // then
            assertThat(participant.getPreferredBlockStatus()).isEqualTo(TimepickParticipantStatus.SUBMITTED);
            assertThat(participant.getPreferredBlock()).isEqualTo(PreferredBlock.BLOCK_12_16);
            verify(userTimepickDefaultService).updatePreferredBlock(USER_ID, PreferredBlock.BLOCK_12_16);
        }

        @Test
        @DisplayName("타임픽이 존재하지 않으면 TIMEPICK_NOT_FOUND 예외 발생")
        void submitPreferredBlock_TimepickNotFound() {
            // given
            PreferredBlockSubmitRequest request = createPreferredBlockSubmitRequest(PreferredBlock.BLOCK_08_12);

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timepickService.submitPreferredBlock(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("타임픽이 FINALIZED 상태면 TIMEPICK_NOT_OPEN 예외 발생")
        void submitPreferredBlock_TimepickFinalized() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.FINALIZED);
            PreferredBlockSubmitRequest request = createPreferredBlockSubmitRequest(PreferredBlock.BLOCK_08_12);

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));

            // when & then
            assertThatThrownBy(() -> timepickService.submitPreferredBlock(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_OPEN);
                    });
        }

        @Test
        @DisplayName("참가자가 아니면 TIMEPICK_NOT_PARTICIPANT 예외 발생")
        void submitPreferredBlock_NotParticipant() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            PreferredBlockSubmitRequest request = createPreferredBlockSubmitRequest(PreferredBlock.BLOCK_08_12);

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timepickService.submitPreferredBlock(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_NOT_PARTICIPANT);
                    });
        }

        @Test
        @DisplayName("이미 제출한 경우 TIMEPICK_PREFERRED_BLOCK_ALREADY_SUBMITTED 예외 발생")
        void submitPreferredBlock_AlreadySubmitted() {
            // given
            Timepick timepick = createTimepick(TimepickStatus.OPEN);
            TimepickParticipant participant = createParticipant(
                    TimepickParticipantStatus.PENDING,
                    TimepickParticipantStatus.SUBMITTED
            );
            PreferredBlockSubmitRequest request = createPreferredBlockSubmitRequest(PreferredBlock.BLOCK_08_12);

            given(timepickRepository.findByTeamRoomId(TEAM_ROOM_ID))
                    .willReturn(Optional.of(timepick));
            given(timepickParticipantRepository.findByTimepickIdAndUserId(TIMEPICK_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when & then
            assertThatThrownBy(() -> timepickService.submitPreferredBlock(TEAM_ROOM_ID, USER_ID, request))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.TIMEPICK_PREFERRED_BLOCK_ALREADY_SUBMITTED);
                    });
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

    private AvailabilitySubmitRequest createAvailabilitySubmitRequest() {
        Boolean[] allFalse = new Boolean[32];
        for (int i = 0; i < 32; i++) {
            allFalse[i] = false;
        }
        Boolean[] mondaySlots = allFalse.clone();
        mondaySlots[0] = true;

        return new AvailabilitySubmitRequest(
                new AvailabilitySubmitRequest.AvailabilityData(
                        allFalse.clone(),  // sunday
                        mondaySlots,       // monday - 첫 슬롯만 true
                        allFalse.clone(),  // tuesday
                        allFalse.clone(),  // wednesday
                        allFalse.clone(),  // thursday
                        allFalse.clone(),  // friday
                        allFalse.clone()   // saturday
                )
        );
    }

    private PreferredBlockSubmitRequest createPreferredBlockSubmitRequest(PreferredBlock preferredBlock) {
        return new PreferredBlockSubmitRequest(preferredBlock);
    }
}
