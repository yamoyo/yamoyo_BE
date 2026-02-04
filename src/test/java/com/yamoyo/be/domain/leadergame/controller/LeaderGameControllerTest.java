package com.yamoyo.be.domain.leadergame.controller;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.response.VolunteerPhaseResponse;
import com.yamoyo.be.domain.leadergame.service.LeaderGameService;
import com.yamoyo.be.domain.leadergame.service.GameStateRedisService;
import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.leadergame.dto.response.UserOnlineResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaderGameController 테스트")
class LeaderGameControllerTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private GameStateRedisService gameStateRedisService;

    @Mock
    private LeaderGameService leaderGameService;

    @InjectMocks
    private LeaderGameController controller;

    private TeamMember createMockTeamMember(Long memberId, Long userId, String userName, TeamRole role) {
        TeamMember member = mock(TeamMember.class);
        User user = mock(User.class);
        TeamRoom teamRoom = mock(TeamRoom.class);

        given(user.getId()).willReturn(userId);
        given(user.getName()).willReturn(userName);
        given(user.getProfileImageId()).willReturn(1L);

        given(member.getId()).willReturn(memberId);
        given(member.getUser()).willReturn(user);
        given(member.getTeamRoom()).willReturn(teamRoom);
        given(member.getTeamRole()).willReturn(role);

        return member;
    }

    @Nested
    @DisplayName("getMembers - 팀룸 멤버 온라인 상태 조회")
    class GetMembersTest {

        @Test
        @DisplayName("멤버 온라인 상태 조회 성공")
        void getMembers_Success() {
            // given
            Long roomId = 1L;
            TeamMember member1 = createMockTeamMember(1L, 100L, "User1", TeamRole.HOST);
            TeamMember member2 = createMockTeamMember(2L, 200L, "User2", TeamRole.MEMBER);
            TeamMember member3 = createMockTeamMember(3L, 300L, "User3", TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomId(roomId))
                    .willReturn(List.of(member1, member2, member3));
            given(gameStateRedisService.getConnectedUsers(roomId))
                    .willReturn(Set.of(100L, 200L)); // User1, User2 온라인

            // when
            ApiResponse<List<UserOnlineResponse>> response = controller.getMembers(roomId);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(3);

            verify(teamMemberRepository).findByTeamRoomId(roomId);
            verify(gameStateRedisService).getConnectedUsers(roomId);
        }

        @Test
        @DisplayName("멤버가 없는 경우 빈 리스트 반환")
        void getMembers_NoMembers_ReturnsEmptyList() {
            // given
            Long roomId = 1L;

            given(teamMemberRepository.findByTeamRoomId(roomId)).willReturn(List.of());
            given(gameStateRedisService.getConnectedUsers(roomId)).willReturn(Set.of());

            // when
            ApiResponse<List<UserOnlineResponse>> response = controller.getMembers(roomId);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("모든 멤버가 오프라인인 경우")
        void getMembers_AllOffline() {
            // given
            Long roomId = 1L;
            TeamMember member1 = createMockTeamMember(1L, 100L, "User1", TeamRole.HOST);
            TeamMember member2 = createMockTeamMember(2L, 200L, "User2", TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomId(roomId))
                    .willReturn(List.of(member1, member2));
            given(gameStateRedisService.getConnectedUsers(roomId)).willReturn(Set.of());

            // when
            ApiResponse<List<UserOnlineResponse>> response = controller.getMembers(roomId);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(2);
        }

        @Test
        @DisplayName("모든 멤버가 온라인인 경우")
        void getMembers_AllOnline() {
            // given
            Long roomId = 1L;
            TeamMember member1 = createMockTeamMember(1L, 100L, "User1", TeamRole.HOST);
            TeamMember member2 = createMockTeamMember(2L, 200L, "User2", TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomId(roomId))
                    .willReturn(List.of(member1, member2));
            given(gameStateRedisService.getConnectedUsers(roomId)).willReturn(Set.of(100L, 200L));

            // when
            ApiResponse<List<UserOnlineResponse>> response = controller.getMembers(roomId);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("startVolunteer - 지원 단계 시작")
    class StartVolunteerTest {

        @Test
        @DisplayName("지원 단계 시작 성공")
        void startVolunteer_Success() {
            // given
            Long roomId = 1L;
            Long userId = 100L;
            JwtTokenClaims claims = new JwtTokenClaims(userId, "host@test.com", "USER");

            List<GameParticipant> participants = List.of(
                    GameParticipant.of(100L, "Host", "1"),
                    GameParticipant.of(200L, "Member", "2")
            );
            VolunteerPhaseResponse mockResponse = VolunteerPhaseResponse.of(
                    System.currentTimeMillis(), 10L, participants
            );

            given(leaderGameService.startVolunteerPhase(roomId, userId)).willReturn(mockResponse);

            // when
            ApiResponse<VolunteerPhaseResponse> response = controller.startVolunteer(roomId, claims);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().durationSeconds()).isEqualTo(10L);
            assertThat(response.getData().participants()).hasSize(2);

            verify(leaderGameService).startVolunteerPhase(roomId, userId);
        }
    }
}
