package com.yamoyo.be.domain.teamroom.scheduler;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamRoomScheduler 단위 테스트")
class TeamRoomSchedulerTest {

    @InjectMocks
    private TeamRoomScheduler teamRoomScheduler;

    @Mock
    private TeamRoomRepository teamRoomRepository;

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
        }
    }
}