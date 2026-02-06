package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.request.JoinTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.*;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import com.yamoyo.be.domain.teamroom.repository.BannedTeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.event.event.NotificationEvent;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamRoomService 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamRoomServiceTest {

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BannedTeamMemberRepository bannedTeamMemberRepository;

    @Mock
    private InviteTokenService inviteTokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TeamRoomService teamRoomService;

    // ==================== 공통 헬퍼 메서드 ====================

    private User createMockUser(Long userId, String email, String name) {
        User user = User.create(email, name);
        return user;
    }

    private TeamRoom createMockTeamRoom(Long teamRoomId, String title) {
        TeamRoom teamRoom = mock(TeamRoom.class);
        given(teamRoom.getId()).willReturn(teamRoomId);
        given(teamRoom.getTitle()).willReturn(title);
        given(teamRoom.getDescription()).willReturn("팀룸 설명");
        given(teamRoom.getDeadline()).willReturn(LocalDateTime.now().plusDays(7));
        given(teamRoom.getLifecycle()).willReturn(Lifecycle.ACTIVE);
        given(teamRoom.getWorkflow()).willReturn(Workflow.PENDING);
        given(teamRoom.getBannerImageId()).willReturn(null);
        given(teamRoom.getCreatedAt()).willReturn(LocalDateTime.now());
        return teamRoom;
    }

    private TeamMember createMockTeamMember(Long memberId, TeamRoom teamRoom, User user, TeamRole role) {
        TeamMember member = mock(TeamMember.class);
        given(member.getId()).willReturn(memberId);
        given(member.getTeamRoom()).willReturn(teamRoom);
        given(member.getUser()).willReturn(user);
        given(member.getTeamRole()).willReturn(role);
        given(member.hasManagementAuthority()).willReturn(
                role == TeamRole.HOST || role == TeamRole.LEADER
        );
        return member;
    }

    // ==================== 팀룸 생성 테스트 ====================

    @Nested
    @DisplayName("팀룸 생성")
    class CreateTeamRoomTest {

        @Test
        @DisplayName("팀룸 생성 성공")
        void createTeamRoom_Success() {
            // given
            Long userId = 1L;
            CreateTeamRoomRequest request = new CreateTeamRoomRequest(
                    "팀룸 제목",
                    "팀룸 설명",
                    LocalDateTime.now().plusDays(7),
                    null
            );

            User user = createMockUser(userId, "test@example.com", "테스트유저");
            
            // 생성 테스트에서는 실제 빌더 사용
            TeamRoom teamRoom = TeamRoom.builder()
                    .title(request.title())
                    .description(request.description())
                    .deadline(request.deadline())
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(teamRoomRepository.save(any(TeamRoom.class))).willReturn(teamRoom);
            given(inviteTokenService.createInviteToken(any())).willReturn("test-token");

            // when
            CreateTeamRoomResponse response = teamRoomService.createTeamRoom(request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.inviteToken()).isEqualTo("test-token");
            assertThat(response.expiresInSeconds()).isEqualTo(86400L);

            verify(userRepository).findById(userId);
            verify(teamRoomRepository).save(any(TeamRoom.class));
            verify(teamMemberRepository).save(any(TeamMember.class));
            verify(inviteTokenService).createInviteToken(any());
        }

        @Test
        @DisplayName("팀룸 생성 실패 - 유저 없음")
        void createTeamRoom_UserNotFound() {
            // given
            Long userId = 999L;
            CreateTeamRoomRequest request = new CreateTeamRoomRequest(
                    "팀룸 제목",
                    "팀룸 설명",
                    LocalDateTime.now().plusDays(7),
                    null
            );

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> teamRoomService.createTeamRoom(request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

            verify(userRepository).findById(userId);
            verify(teamRoomRepository, never()).save(any());
            verify(teamMemberRepository, never()).save(any());
        }
    }

    // ==================== 팀룸 입장 테스트 ====================

    @Nested
    @DisplayName("팀룸 입장")
    class JoinTeamRoomTest {

        @Test
        @DisplayName("신규 멤버 팀룸 입장 성공")
        void joinTeamRoom_NewMember_Success() {
            // given
            Long userId = 1L;
            String token = "test-token";
            Long teamRoomId = 10L;

            JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);
            User user = createMockUser(userId, "test@example.com", "테스트유저");
            
            // 입장 테스트에서는 실제 빌더 사용
            TeamRoom teamRoom = TeamRoom.builder()
                    .title("팀룸 제목")
                    .description("팀룸 설명")
                    .deadline(LocalDateTime.now().plusDays(7))
                    .build();
            TeamMember newMember = TeamMember.createMember(teamRoom, user);

            given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(1L);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(teamMemberRepository.save(any(TeamMember.class))).willReturn(newMember);

            // when
            JoinTeamRoomResponse response = teamRoomService.joinTeamRoom(request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.teamRoomId()).isEqualTo(teamRoomId);
            assertThat(response.joinResult()).isEqualTo(JoinTeamRoomResponse.JoinResult.JOINED);

            verify(inviteTokenService).getTeamRoomIdByToken(token);
            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamMemberRepository).countByTeamRoomId(teamRoomId);
            verify(userRepository).findById(userId);
            verify(teamMemberRepository).save(any(TeamMember.class));
            verify(eventPublisher).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("기존 멤버 재입장 성공")
        void joinTeamRoom_ExistingMember_Success() {
            // given
            Long userId = 1L;
            String token = "test-token";
            Long teamRoomId = 10L;

            JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");
            TeamMember existing = mock(TeamMember.class);
            given(existing.getId()).willReturn(99L);

            given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(existing));

            // when
            JoinTeamRoomResponse response = teamRoomService.joinTeamRoom(request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.teamRoomId()).isEqualTo(teamRoomId);
            assertThat(response.memberId()).isEqualTo(99L);
            assertThat(response.joinResult()).isEqualTo(JoinTeamRoomResponse.JoinResult.ENTERED);

            verify(teamMemberRepository, never()).countByTeamRoomId(anyLong());
            verify(userRepository, never()).findById(anyLong());
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("팀룸 입장 실패 - 정원 초과")
        void joinTeamRoom_TeamRoomFull() {
            // given
            Long userId = 1L;
            String token = "test-token";
            Long teamRoomId = 10L;

            JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");

            given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(12L);

            // when / then
            assertThatThrownBy(() -> teamRoomService.joinTeamRoom(request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.TEAMROOM_FULL.getMessage());

            verify(teamMemberRepository, never()).save(any());
            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("팀룸 입장 실패 - 초대 토큰 유효하지 않음")
        void joinTeamRoom_InvalidInviteToken() {
            // given
            Long userId = 1L;
            String token = "invalid-token";
            JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);

            given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(null);

            // when / then
            assertThatThrownBy(() -> teamRoomService.joinTeamRoom(request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.INVITE_INVALID.getMessage());

            verify(teamRoomRepository, never()).findById(anyLong());
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("팀룸 입장 실패 - 팀룸 없음")
        void joinTeamRoom_TeamRoomNotFound() {
            // given
            Long userId = 1L;
            String token = "test-token";
            Long teamRoomId = 999L;

            JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);

            given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> teamRoomService.joinTeamRoom(request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.TEAMROOM_NOT_FOUND.getMessage());

            verify(teamMemberRepository, never()).findByTeamRoomIdAndUserId(anyLong(), anyLong());
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("팀룸 입장 실패 - 진행 중 입장 불가 (LEADER_SELECTION)")
        void joinTeamRoom_JoinForbidden_LeaderSelection() {
            // given
            Long userId = 1L;
            String token = "test-token";
            Long teamRoomId = 10L;

            JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");
            
            // Workflow를 LEADER_SELECTION으로 설정하기 위해 mock 재정의
            TeamRoom mockTeamRoom = mock(TeamRoom.class);
            given(mockTeamRoom.getId()).willReturn(teamRoomId);
            given(mockTeamRoom.getTitle()).willReturn("팀룸 제목");
            given(mockTeamRoom.getLifecycle()).willReturn(Lifecycle.ACTIVE);
            given(mockTeamRoom.getWorkflow()).willReturn(Workflow.LEADER_SELECTION);

            given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(mockTeamRoom));

            // when / then
            assertThatThrownBy(() -> teamRoomService.joinTeamRoom(request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.TEAMROOM_JOIN_FORBIDDEN.getMessage());

            verify(teamMemberRepository, never()).findByTeamRoomIdAndUserId(anyLong(), anyLong());
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("팀룸 입장 실패 - 밴된 유저")
        void joinTeamRoom_Banned() {
            // given
            String token = "valid_token";
            Long userId = 1L;
            Long teamRoomId = 1L;

            given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(bannedTeamMemberRepository.existsByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(true); // 밴된 유저

            JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);

            // when / then
            assertThatThrownBy(() -> teamRoomService.joinTeamRoom(request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.BANNED_MEMBER.getMessage());

            verify(teamMemberRepository, never()).save(any());
        }
    }

    // ==================== 팀룸 목록 조회 테스트 ====================

    @Nested
    @DisplayName("팀룸 목록 조회")
    class GetTeamRoomListTest {

        @ParameterizedTest
        @EnumSource(value = Lifecycle.class, names = {"ACTIVE", "ARCHIVED"})
        @DisplayName("팀룸 목록 조회 성공 - ACTIVE/ARCHIVED")
        void getTeamRoomList_Success(Lifecycle lifecycle) {
            // given
            Long userId = 1L;
            User user = createMockUser(userId, "test@example.com", "테스트유저");
            
            TeamRoom teamRoom1 = createMockTeamRoom(10L, "팀룸1");
            TeamRoom teamRoom2 = createMockTeamRoom(20L, "팀룸2");
            
            TeamMember member1 = createMockTeamMember(1L, teamRoom1, user, TeamRole.HOST);
            TeamMember member2 = createMockTeamMember(2L, teamRoom2, user, TeamRole.MEMBER);

            given(teamMemberRepository.findTeamRoomsByUserIdAndLifecycle(userId, lifecycle))
                    .willReturn(Arrays.asList(teamRoom1, teamRoom2));
            given(teamMemberRepository.findByTeamRoomId(10L))
                    .willReturn(Arrays.asList(member1));
            given(teamMemberRepository.findByTeamRoomId(20L))
                    .willReturn(Arrays.asList(member2));

            // when
            List<TeamRoomListResponse> response = teamRoomService.getTeamRoomList(userId, lifecycle);

            // then
            assertThat(response).isNotNull();
            assertThat(response).hasSize(2);
            assertThat(response.get(0).teamRoomId()).isEqualTo(10L);
            assertThat(response.get(1).teamRoomId()).isEqualTo(20L);

            verify(teamMemberRepository).findTeamRoomsByUserIdAndLifecycle(userId, lifecycle);
            verify(teamMemberRepository, times(2)).findByTeamRoomId(anyLong());
        }
    }

    // ==================== 팀룸 상세 조회 테스트 ====================

    @Nested
    @DisplayName("팀룸 상세 조회")
    class GetTeamRoomDetailTest {

        @Test
        @DisplayName("팀룸 상세 조회 성공")
        void getTeamRoomDetail_Success() {
            // given
            Long userId = 1L;
            Long teamRoomId = 10L;

            User user = createMockUser(userId, "test@example.com", "테스트유저");
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");
            TeamMember myMember = createMockTeamMember(1L, teamRoom, user, TeamRole.MEMBER);
            
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(myMember));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(Arrays.asList(myMember));

            // when
            TeamRoomDetailResponse response = teamRoomService.getTeamRoomDetail(teamRoomId, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.teamRoomId()).isEqualTo(teamRoomId);
            assertThat(response.title()).isEqualTo("팀룸 제목");
            assertThat(response.myRole()).isEqualTo(TeamRole.MEMBER);

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamMemberRepository).findByTeamRoomId(teamRoomId);
        }

        @Test
        @DisplayName("팀룸 상세 조회 실패 - 권한 없음")
        void getTeamRoomDetail_NotTeamMember() {
            // given
            Long userId = 1L;
            Long teamRoomId = 10L;

            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> teamRoomService.getTeamRoomDetail(teamRoomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MEMBER.getMessage());

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamMemberRepository, never()).findByTeamRoomId(anyLong());
        }
    }

    // ==================== 팀룸 수정 테스트 ====================

    @Nested
    @DisplayName("팀룸 수정")
    class UpdateTeamRoomTest {

        @Test
        @DisplayName("팀룸 수정 성공")
        void updateTeamRoom_Success() {
            // given
            Long userId = 1L;
            Long teamRoomId = 10L;
            CreateTeamRoomRequest request = new CreateTeamRoomRequest(
                    "수정된 제목",
                    "수정된 설명",
                    LocalDateTime.now().plusDays(10),
                    2L
            );

            User user = createMockUser(userId, "test@example.com", "테스트유저");
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "원래 제목");
            TeamMember leader = createMockTeamMember(1L, teamRoom, user, TeamRole.LEADER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(leader));

            // when
            teamRoomService.updateTeamRoom(teamRoomId, request, userId);

            // then
            LocalDateTime expectedDeadline = request.deadline().toLocalDate().atTime(23, 59, 59);

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamRoom).update(
                    request.title(),
                    request.description(),
                    expectedDeadline,
                    request.bannerImageId()
            );
        }

        @Test
        @DisplayName("팀룸 수정 실패 - 권한 없음")
        void updateTeamRoom_NotTeamManager() {
            // given
            Long userId = 1L;
            Long teamRoomId = 10L;
            CreateTeamRoomRequest request = new CreateTeamRoomRequest(
                    "수정된 제목",
                    "수정된 설명",
                    LocalDateTime.now().plusDays(10),
                    null
            );

            User user = createMockUser(userId, "test@example.com", "테스트유저");
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "원래 제목");
            TeamMember member = createMockTeamMember(1L, teamRoom, user, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));

            // when / then
            assertThatThrownBy(() -> teamRoomService.updateTeamRoom(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MANAGER.getMessage());

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamRoom, never()).update(any(), any(), any(), any());
        }
    }

    // ==================== 팀룸 삭제 테스트 ====================

    @Nested
    @DisplayName("팀룸 삭제")
    class DeleteTeamRoomTest {

        @Test
        @DisplayName("팀룸 삭제 성공 - 소프트 삭제")
        void deleteTeamRoom_Success() {
            // given
            Long userId = 1L;
            Long teamRoomId = 10L;

            User user = createMockUser(userId, "test@example.com", "테스트유저");
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");
            TeamMember host = createMockTeamMember(1L, teamRoom, user, TeamRole.HOST);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(host));

            // when
            teamRoomService.deleteTeamRoom(teamRoomId, userId);

            // then
            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamRoom).delete();
        }

        @Test
        @DisplayName("팀룸 삭제 실패 - 권한 없음")
        void deleteTeamRoom_NotTeamManager() {
            // given
            Long userId = 1L;
            Long teamRoomId = 10L;

            User user = createMockUser(userId, "test@example.com", "테스트유저");
            TeamRoom teamRoom = createMockTeamRoom(teamRoomId, "팀룸 제목");
            TeamMember member = createMockTeamMember(1L, teamRoom, user, TeamRole.MEMBER);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));

            // when / then
            assertThatThrownBy(() -> teamRoomService.deleteTeamRoom(teamRoomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasMessageContaining(ErrorCode.NOT_TEAM_MANAGER.getMessage());

            verify(teamRoomRepository).findById(teamRoomId);
            verify(teamMemberRepository).findByTeamRoomIdAndUserId(teamRoomId, userId);
            verify(teamRoom, never()).delete();
        }
    }
}
