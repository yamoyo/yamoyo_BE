package com.yamoyo.be.domain.teamroom.scheduler;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.event.event.NotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamRoomScheduler 단위 테스트")
class TeamRoomSchedulerTest {

    @InjectMocks
    private TeamRoomScheduler teamRoomScheduler;

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("아카이빙 처리")
    class ArchiveExpiredTeamRooms {

        @Test
        @DisplayName("성공: 아카이빙 대상이 없는 경우")
        void archiveExpiredTeamRooms_Success_NoTeamRoomsToArchive() {
            // given
            given(teamRoomRepository.findByLifecycleAndDeadlineBefore(any(), any()))
                    .willReturn(Collections.emptyList());

            // when
            teamRoomScheduler.archiveExpiredTeamRooms();

            // then
            then(teamRoomRepository).should().findByLifecycleAndDeadlineBefore(
                    eq(Lifecycle.ACTIVE),
                    any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("성공: 아카이빙 대상이 1개인 경우")
        void archiveExpiredTeamRooms_Success_OneTeamRoom() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            given(teamRoom.getId()).willReturn(1L);

            given(teamRoomRepository.findByLifecycleAndDeadlineBefore(any(), any()))
                    .willReturn(List.of(teamRoom));

            // when
            teamRoomScheduler.archiveExpiredTeamRooms();

            // then
            then(teamRoomRepository).should().findByLifecycleAndDeadlineBefore(
                    eq(Lifecycle.ACTIVE),
                    any(LocalDateTime.class)
            );
            then(teamRoom).should().archive();
            then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("성공: 아카이빙 대상이 여러 개인 경우")
        void archiveExpiredTeamRooms_Success_MultipleTeamRooms() {
            // given
            TeamRoom teamRoom1 = mock(TeamRoom.class);
            given(teamRoom1.getId()).willReturn(1L);
            given(teamRoom1.getTitle()).willReturn("팀룸1");

            TeamRoom teamRoom2 = mock(TeamRoom.class);
            given(teamRoom2.getId()).willReturn(2L);
            given(teamRoom2.getTitle()).willReturn("팀룸2");

            TeamRoom teamRoom3 = mock(TeamRoom.class);
            given(teamRoom3.getId()).willReturn(3L);
            given(teamRoom3.getTitle()).willReturn("팀룸3");

            given(teamRoomRepository.findByLifecycleAndDeadlineBefore(any(), any()))
                    .willReturn(List.of(teamRoom1, teamRoom2, teamRoom3));

            // when
            teamRoomScheduler.archiveExpiredTeamRooms();

            // then
            then(teamRoom1).should().archive();
            then(teamRoom2).should().archive();
            then(teamRoom3).should().archive();
            then(eventPublisher).should(times(3)).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("성공: 일부 팀룸 아카이빙 실패 시 다른 팀룸은 계속 진행")
        void archiveExpiredTeamRooms_Success_PartialFailure() {
            // given
            TeamRoom teamRoom1 = mock(TeamRoom.class);
            given(teamRoom1.getId()).willReturn(1L);
            willThrow(new IllegalStateException("아카이빙 실패"))
                    .given(teamRoom1).archive();

            TeamRoom teamRoom2 = mock(TeamRoom.class);
            given(teamRoom2.getId()).willReturn(2L);

            TeamRoom teamRoom3 = mock(TeamRoom.class);
            given(teamRoom3.getId()).willReturn(3L);

            given(teamRoomRepository.findByLifecycleAndDeadlineBefore(any(), any()))
                    .willReturn(List.of(teamRoom1, teamRoom2, teamRoom3));

            // when
            teamRoomScheduler.archiveExpiredTeamRooms();

            // then
            then(teamRoom1).should().archive();
            then(teamRoom2).should().archive();
            then(teamRoom3).should().archive();
            then(eventPublisher).should(times(2)).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("성공: 모든 팀룸 아카이빙 실패")
        void archiveExpiredTeamRooms_Success_AllFailures() {
            // given
            TeamRoom teamRoom1 = mock(TeamRoom.class);
            given(teamRoom1.getId()).willReturn(1L);
            willThrow(new IllegalStateException("아카이빙 실패"))
                    .given(teamRoom1).archive();

            TeamRoom teamRoom2 = mock(TeamRoom.class);
            given(teamRoom2.getId()).willReturn(2L);
            willThrow(new IllegalStateException("아카이빙 실패"))
                    .given(teamRoom2).archive();

            given(teamRoomRepository.findByLifecycleAndDeadlineBefore(any(), any()))
                    .willReturn(List.of(teamRoom1, teamRoom2));

            // when
            teamRoomScheduler.archiveExpiredTeamRooms();

            // then
            then(teamRoom1).should().archive();
            then(teamRoom2).should().archive();
            then(eventPublisher).should(never()).publishEvent(any(NotificationEvent.class));
        }
    }

    @Nested
    @DisplayName("마감 D-1 알림")
    class SendDeadlineReminder {

        @Test
        @DisplayName("성공: D-1 팀룸이 없는 경우")
        void sendDeadlineReminder_Success_NoRooms() {
            // given
            given(teamRoomRepository.findByLifecycleAndDeadline(any(), any()))
                    .willReturn(Collections.emptyList());

            // when
            teamRoomScheduler.sendDeadlineReminder();

            // then
            then(teamRoomRepository).should().findByLifecycleAndDeadline(
                    eq(Lifecycle.ACTIVE),
                    any(LocalDateTime.class)
            );
            then(eventPublisher).should(never()).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("성공: D-1 팀룸이 있는 경우 알림 발송")
        void sendDeadlineReminder_Success_WithRooms() {
            // given
            TeamRoom teamRoom1 = mock(TeamRoom.class);
            given(teamRoom1.getId()).willReturn(1L);

            TeamRoom teamRoom2 = mock(TeamRoom.class);
            given(teamRoom2.getId()).willReturn(2L);

            given(teamRoomRepository.findByLifecycleAndDeadline(any(), any()))
                    .willReturn(List.of(teamRoom1, teamRoom2));

            // when
            teamRoomScheduler.sendDeadlineReminder();

            // then
            then(eventPublisher).should(times(2)).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("성공: 일부 팀룸 알림 발송 실패 시 다른 팀룸은 계속 진행")
        void sendDeadlineReminder_Success_PartialFailure() {
            // given
            TeamRoom teamRoom1 = mock(TeamRoom.class);
            given(teamRoom1.getId()).willReturn(1L);

            TeamRoom teamRoom2 = mock(TeamRoom.class);
            given(teamRoom2.getId()).willReturn(2L);

            given(teamRoomRepository.findByLifecycleAndDeadline(any(), any()))
                    .willReturn(List.of(teamRoom1, teamRoom2));

            // 첫 호출 실패, 두 번째 호출 성공
            willThrow(new RuntimeException("알림 발송 실패"))
                    .willDoNothing()
                    .given(eventPublisher).publishEvent(any(NotificationEvent.class));

            // when
            teamRoomScheduler.sendDeadlineReminder();

            // then: 첫 번째 호출에서 예외 발생하지만 루프는 계속 실행
            then(eventPublisher).should(times(2)).publishEvent(any(NotificationEvent.class));
        }
    }
}