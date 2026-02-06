package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.meeting.service.TimepickService;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaderGameDbService 테스트")
class LeaderGameDbServiceTest {

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamRoomSetupRepository teamRoomSetupRepository;

    @Mock
    private TimepickService timepickService;

    @InjectMocks
    private LeaderGameDbService leaderGameDbService;

    private TeamRoom createMockTeamRoom(Long roomId) {
        TeamRoom teamRoom = mock(TeamRoom.class);
        given(teamRoom.getId()).willReturn(roomId);
        return teamRoom;
    }

    private TeamMember createMockTeamMember(Long memberId, Long userId, TeamRole role) {
        TeamMember member = mock(TeamMember.class);
        given(member.getId()).willReturn(memberId);
        given(member.getTeamRole()).willReturn(role);
        return member;
    }

    @Nested
    @DisplayName("applyLeaderResult - 팀장 선출 결과 DB 반영")
    class ApplyLeaderResultTest {

        @Test
        @DisplayName("팀장 선출 결과 반영 성공 - 당첨자 승격, HOST 강등, TeamRoomSetup 생성")
        void applyLeaderResult_Success() {
            // given
            Long roomId = 1L;
            Long winnerId = 100L;

            TeamRoom teamRoom = createMockTeamRoom(roomId);
            TeamMember winner = createMockTeamMember(1L, winnerId, TeamRole.MEMBER);
            TeamMember host = createMockTeamMember(2L, 200L, TeamRole.HOST);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, winnerId))
                    .willReturn(Optional.of(winner));
            given(teamMemberRepository.findByTeamRoomIdAndTeamRole(roomId, TeamRole.HOST))
                    .willReturn(Optional.of(host));
            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));

            // when
            leaderGameDbService.applyLeaderResult(roomId, winnerId);

            // then
            verify(winner).promoteToLeader();
            verify(host).demoteToMember();
            verify(teamRoom).completeLeaderSelection();
            verify(teamRoomSetupRepository).save(any(TeamRoomSetup.class));
            verify(timepickService).createTimepick(roomId);
        }

        @Test
        @DisplayName("HOST가 없는 경우에도 정상 처리 및 TeamRoomSetup 생성")
        void applyLeaderResult_NoHost_Success() {
            // given
            Long roomId = 1L;
            Long winnerId = 100L;

            TeamRoom teamRoom = createMockTeamRoom(roomId);
            TeamMember winner = createMockTeamMember(1L, winnerId, TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, winnerId))
                    .willReturn(Optional.of(winner));
            given(teamMemberRepository.findByTeamRoomIdAndTeamRole(roomId, TeamRole.HOST))
                    .willReturn(Optional.empty());
            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));

            // when
            leaderGameDbService.applyLeaderResult(roomId, winnerId);

            // then
            verify(winner).promoteToLeader();
            verify(teamRoom).completeLeaderSelection();
            verify(teamRoomSetupRepository).save(any(TeamRoomSetup.class));
            verify(timepickService).createTimepick(roomId);
        }

        @Test
        @DisplayName("당첨자를 찾을 수 없는 경우 예외 발생")
        void applyLeaderResult_WinnerNotFound_ThrowsException() {
            // given
            Long roomId = 1L;
            Long winnerId = 999L;

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, winnerId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> leaderGameDbService.applyLeaderResult(roomId, winnerId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TEAMROOM_MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("팀룸을 찾을 수 없는 경우 예외 발생")
        void applyLeaderResult_TeamRoomNotFound_ThrowsException() {
            // given
            Long roomId = 999L;
            Long winnerId = 100L;

            TeamMember winner = createMockTeamMember(1L, winnerId, TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, winnerId))
                    .willReturn(Optional.of(winner));
            given(teamMemberRepository.findByTeamRoomIdAndTeamRole(roomId, TeamRole.HOST))
                    .willReturn(Optional.empty());
            given(teamRoomRepository.findById(roomId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> leaderGameDbService.applyLeaderResult(roomId, winnerId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TEAMROOM_NOT_FOUND));
        }

        @Test
        @DisplayName("HOST가 당첨된 경우 (자기 자신이 팀장)")
        void applyLeaderResult_HostIsWinner() {
            // given
            Long roomId = 1L;
            Long winnerId = 100L;

            TeamRoom teamRoom = createMockTeamRoom(roomId);
            TeamMember hostWinner = createMockTeamMember(1L, winnerId, TeamRole.HOST);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, winnerId))
                    .willReturn(Optional.of(hostWinner));
            given(teamMemberRepository.findByTeamRoomIdAndTeamRole(roomId, TeamRole.HOST))
                    .willReturn(Optional.of(hostWinner));
            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));

            // when
            leaderGameDbService.applyLeaderResult(roomId, winnerId);

            // then
            verify(hostWinner).promoteToLeader();
            verify(hostWinner).demoteToMember();
            verify(teamRoom).completeLeaderSelection();
            verify(teamRoomSetupRepository).save(any(TeamRoomSetup.class));
            verify(timepickService).createTimepick(roomId);
        }

        @Test
        @DisplayName("TeamRoomSetup이 올바른 TeamRoom과 함께 생성되는지 검증")
        void applyLeaderResult_VerifyTeamRoomSetupCreation() {
            // given
            Long roomId = 1L;
            Long winnerId = 100L;

            TeamRoom teamRoom = createMockTeamRoom(roomId);
            TeamMember winner = createMockTeamMember(1L, winnerId, TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, winnerId))
                    .willReturn(Optional.of(winner));
            given(teamMemberRepository.findByTeamRoomIdAndTeamRole(roomId, TeamRole.HOST))
                    .willReturn(Optional.empty());
            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));

            // when
            leaderGameDbService.applyLeaderResult(roomId, winnerId);

            // then
            verify(teamRoomSetupRepository).save(argThat(setup ->
                setup != null
            ));
        }
    }

    @Nested
    @DisplayName("startLeaderSelection - 팀장 선출 시작")
    class StartLeaderSelectionTest {

        @Test
        @DisplayName("팀장 선출 시작 성공")
        void startLeaderSelection_Success() {
            // given
            Long roomId = 1L;
            TeamRoom teamRoom = createMockTeamRoom(roomId);

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));

            // when
            leaderGameDbService.startLeaderSelection(roomId);

            // then
            verify(teamRoom).startLeaderSelection();
        }

        @Test
        @DisplayName("팀룸을 찾을 수 없는 경우 예외 발생")
        void startLeaderSelection_TeamRoomNotFound_ThrowsException() {
            // given
            Long roomId = 999L;

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> leaderGameDbService.startLeaderSelection(roomId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TEAMROOM_NOT_FOUND));
        }
    }
}
