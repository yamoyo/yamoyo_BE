package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.entity.BannedTeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import com.yamoyo.be.domain.teamroom.repository.BannedTeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TeamMemberService 테스트")
class TeamMemberServiceTest {

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private BannedTeamMemberRepository bannedTeamMemberRepository;

    @InjectMocks
    private TeamMemberService teamMemberService;

    // ==================== 공통 헬퍼 메서드 ====================

    private TeamRoom createMockTeamRoom(Long teamRoomId, Workflow workflow) {
        TeamRoom teamRoom = mock(TeamRoom.class);
        given(teamRoom.getId()).willReturn(teamRoomId);
        given(teamRoom.getLifecycle()).willReturn(Lifecycle.ACTIVE);
        given(teamRoom.getWorkflow()).willReturn(workflow);
        return teamRoom;
    }

    private TeamMember createMockTeamMember(Long memberId, Long teamRoomId, TeamRole role) {
        TeamMember member = mock(TeamMember.class);
        TeamRoom teamRoom = mock(TeamRoom.class);
        given(teamRoom.getId()).willReturn(teamRoomId);

        given(member.getId()).willReturn(memberId);
        given(member.getTeamRoom()).willReturn(teamRoom);
        given(member.getTeamRole()).willReturn(role);
        given(member.hasManagementAuthority()).willReturn(
                role == TeamRole.HOST || role == TeamRole.LEADER
        );
        return member;
    }

    // ==================== 팀룸 나가기 테스트 ====================

    @Nested
    @DisplayName("팀룸 나가기")
    class LeaveTeamRoomTest {

        @Test
        @DisplayName("일반 팀원 나가기 성공")
        void leaveTeamRoom_Member_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember member = createMockTeamMember(1L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(3L);

            // when
            teamMemberService.leaveTeamRoom(teamRoomId, userId);

            // then
            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamMemberRepository).countByTeamRoomId(teamRoomId);
            verify(teamMemberRepository).delete(member);
        }

        @Test
        @DisplayName("관리자 나가기 성공 - 권한 자동 위임")
        void leaveTeamRoom_Leader_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember leader = createMockTeamMember(1L, teamRoomId, TeamRole.LEADER);
            TeamMember member2 = createMockTeamMember(2L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(leader));
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(2L);
            given(teamMemberRepository.findById(1L)).willReturn(Optional.of(leader));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(leader, member2));

            // when
            teamMemberService.leaveTeamRoom(teamRoomId, userId);

            // then
            verify(teamMemberRepository).delete(leader);
            verify(member2).changeTeamRole(TeamRole.LEADER); // 권한 위임 확인
        }

        @Test
        @DisplayName("팀룸 나가기 실패 - 진행 중")
        void leaveTeamRoom_WorkflowForbidden() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.LEADER_SELECTION);
            TeamMember member = createMockTeamMember(1L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));

            // when / then
            assertThatThrownBy(() -> teamMemberService.leaveTeamRoom(teamRoomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.TEAMROOM_LEAVE_FORBIDDEN.getMessage());

            verify(teamMemberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("팀룸 나가기 실패 - 마지막 팀원")
        void leaveTeamRoom_LastMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember member = createMockTeamMember(1L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(1L);

            // when / then
            assertThatThrownBy(() -> teamMemberService.leaveTeamRoom(teamRoomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.TEAMROOM_LEAVE_FORBIDDEN.getMessage());

            verify(teamMemberRepository, never()).delete(any());
        }
    }

    // ==================== 팀원 강퇴 테스트 ====================

    @Nested
    @DisplayName("팀원 강퇴")
    class KickMemberTest {

        @Test
        @DisplayName("팀원 강퇴 성공")
        void kickMember_Success() {
            // given
            Long teamRoomId = 1L;
            Long kickerId = 1L;
            Long targetMemberId = 2L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember kicker = createMockTeamMember(1L, teamRoomId, TeamRole.LEADER);
            TeamMember target = createMockTeamMember(2L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, kickerId))
                    .willReturn(Optional.of(kicker));
            given(teamMemberRepository.findById(targetMemberId)).willReturn(Optional.of(target));

            // when
            teamMemberService.kickMember(teamRoomId, kickerId, targetMemberId);

            // then
            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, kickerId);
            verify(teamMemberRepository).findById(targetMemberId);
            verify(bannedTeamMemberRepository).save(any(BannedTeamMember.class));
            verify(teamMemberRepository).delete(target);
        }

        @Test
        @DisplayName("팀원 강퇴 실패 - 권한 없음")
        void kickMember_NotManager() {
            // given
            Long teamRoomId = 1L;
            Long kickerId = 1L;
            Long targetMemberId = 2L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember kicker = createMockTeamMember(1L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, kickerId))
                    .willReturn(Optional.of(kicker));

            // when / then
            assertThatThrownBy(() -> teamMemberService.kickMember(teamRoomId, kickerId, targetMemberId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MANAGER.getMessage());

            verify(teamMemberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("팀원 강퇴 실패 - 자기 자신")
        void kickMember_CannotKickSelf() {
            // given
            Long teamRoomId = 1L;
            Long kickerId = 1L;
            Long targetMemberId = 1L; // 같은 ID

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember kicker = createMockTeamMember(1L, teamRoomId, TeamRole.LEADER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, kickerId))
                    .willReturn(Optional.of(kicker));
            given(teamMemberRepository.findById(targetMemberId)).willReturn(Optional.of(kicker));

            // when / then
            assertThatThrownBy(() -> teamMemberService.kickMember(teamRoomId, kickerId, targetMemberId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_KICK_SELF.getMessage());

            verify(teamMemberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("팀원 강퇴 실패 - 관리자 강퇴")
        void kickMember_CannotKickManager() {
            // given
            Long teamRoomId = 1L;
            Long kickerId = 1L;
            Long targetMemberId = 2L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember kicker = createMockTeamMember(1L, teamRoomId, TeamRole.LEADER);
            TeamMember target = createMockTeamMember(2L, teamRoomId, TeamRole.HOST);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, kickerId))
                    .willReturn(Optional.of(kicker));
            given(teamMemberRepository.findById(targetMemberId)).willReturn(Optional.of(target));

            // when / then
            assertThatThrownBy(() -> teamMemberService.kickMember(teamRoomId, kickerId, targetMemberId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_KICK_MANAGER.getMessage());

            verify(teamMemberRepository, never()).delete(any());
        }
    }

    // ==================== 팀장 변경 테스트 ====================

    @Nested
    @DisplayName("팀장 변경")
    class ChangeLeaderTest {

        @Test
        @DisplayName("팀장 변경 성공 - LEADER → LEADER")
        void changeLeader_Success() {
            // given
            Long teamRoomId = 1L;
            Long currentManagerId = 1L;
            Long newManagerMemberId = 2L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember currentLeader = createMockTeamMember(1L, teamRoomId, TeamRole.LEADER);
            TeamMember newLeader = createMockTeamMember(2L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, currentManagerId))
                    .willReturn(Optional.of(currentLeader));
            given(teamMemberRepository.findById(newManagerMemberId)).willReturn(Optional.of(newLeader));

            // when
            teamMemberService.changeLeader(teamRoomId, currentManagerId, newManagerMemberId);

            // then
            verify(currentLeader).changeTeamRole(TeamRole.MEMBER);
            verify(newLeader).changeTeamRole(TeamRole.LEADER);
        }

        @Test
        @DisplayName("팀장 변경 실패 - 권한 없음")
        void changeLeader_NotManager() {
            // given
            Long teamRoomId = 1L;
            Long currentManagerId = 1L;
            Long newManagerMemberId = 2L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember currentMember = createMockTeamMember(1L, teamRoomId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, currentManagerId))
                    .willReturn(Optional.of(currentMember));

            // when / then
            assertThatThrownBy(() -> teamMemberService.changeLeader(teamRoomId, currentManagerId, newManagerMemberId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MANAGER.getMessage());

            verify(currentMember, never()).changeTeamRole(any());
        }

        @Test
        @DisplayName("팀장 변경 실패 - 자기 자신에게 위임")
        void changeLeader_CannotDelegateToSelf() {
            // given
            Long teamRoomId = 1L;
            Long currentManagerId = 1L;
            Long newManagerMemberId = 1L; // 같은 ID

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, Workflow.PENDING);
            TeamMember currentLeader = createMockTeamMember(1L, teamRoomId, TeamRole.LEADER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, currentManagerId))
                    .willReturn(Optional.of(currentLeader));
            given(teamMemberRepository.findById(newManagerMemberId)).willReturn(Optional.of(currentLeader));

            // when / then
            assertThatThrownBy(() -> teamMemberService.changeLeader(teamRoomId, currentManagerId, newManagerMemberId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_DELEGATE_TO_SELF.getMessage());

            verify(currentLeader, never()).changeTeamRole(any());
        }
    }
}