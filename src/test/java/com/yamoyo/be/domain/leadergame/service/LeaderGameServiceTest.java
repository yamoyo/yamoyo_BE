package com.yamoyo.be.domain.leadergame.service;

import com.yamoyo.be.domain.leadergame.dto.GameParticipant;
import com.yamoyo.be.domain.leadergame.dto.message.GameResultPayload;
import com.yamoyo.be.domain.leadergame.dto.message.ReloadPayload;
import com.yamoyo.be.domain.leadergame.dto.response.VolunteerPhaseResponse;
import com.yamoyo.be.domain.leadergame.enums.GamePhase;
import com.yamoyo.be.domain.leadergame.enums.GameType;
import com.yamoyo.be.domain.notification.entity.NotificationType;
import com.yamoyo.be.event.event.NotificationEvent;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.TeamRole;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaderGameService 테스트")
class LeaderGameServiceTest {

    @Mock
    private GameStateRedisService redisService;

    @Mock
    private LeaderGameDbService dbService;

    @Mock
    private LadderGameService ladderGameService;

    @Mock
    private RouletteGameService rouletteGameService;

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LeaderGameService leaderGameService;

    // ==================== 공통 헬퍼 메서드 ====================

    private TeamRoom createMockTeamRoom(Long roomId, Workflow workflow) {
        TeamRoom teamRoom = mock(TeamRoom.class);
        given(teamRoom.getId()).willReturn(roomId);
        given(teamRoom.getWorkflow()).willReturn(workflow);
        return teamRoom;
    }

    private User createMockUser(Long userId, String name) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(user.getName()).willReturn(name);
        given(user.getProfileImageId()).willReturn(1L);
        return user;
    }

    private TeamMember createMockTeamMember(Long memberId, Long roomId, Long userId, TeamRole role) {
        TeamMember member = mock(TeamMember.class);
        TeamRoom teamRoom = mock(TeamRoom.class);
        User user = createMockUser(userId, "User" + userId);

        given(teamRoom.getId()).willReturn(roomId);
        given(member.getId()).willReturn(memberId);
        given(member.getTeamRoom()).willReturn(teamRoom);
        given(member.getUser()).willReturn(user);
        given(member.getTeamRole()).willReturn(role);
        given(member.hasManagementAuthority()).willReturn(role == TeamRole.HOST);

        return member;
    }

    private List<GameParticipant> createParticipants(int count) {
        List<GameParticipant> participants = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            participants.add(GameParticipant.of((long) i, "User" + i, String.valueOf(i)));
        }
        return participants;
    }

    // ==================== 재접속 테스트 ====================

    @Nested
    @DisplayName("handleReload - 새로고침/재접속")
    class HandleReloadTest {

        @Test
        @DisplayName("재접속 성공 - 게임 진행 중에도 가능")
        void handleReload_Success() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            User user = createMockUser(userId, "TestUser");
            TeamMember teamMember = createMockTeamMember(1L, roomId, userId, TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.of(teamMember));
            given(teamMemberRepository.findByTeamRoomId(roomId)).willReturn(List.of(teamMember));
            given(redisService.getConnectedUsers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(redisService.getPhaseStartTime(roomId)).willReturn(System.currentTimeMillis());
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L));
            given(redisService.getVotedUsers(roomId)).willReturn(Set.of(1L));
            given(redisService.getSelectedGame(roomId)).willReturn(null);
            given(redisService.getWinner(roomId)).willReturn(null);

            // when
            ReloadPayload result = leaderGameService.handleReload(roomId, user);

            // then
            assertThat(result).isNotNull();
            assertThat(result.currentUser().userId()).isEqualTo(userId);
            assertThat(result.currentPhase()).isEqualTo(GamePhase.VOLUNTEER);
            verify(redisService).addConnection(roomId, userId);
            verify(messagingTemplate).convertAndSend(eq("/sub/room/" + roomId), any(Object.class));
        }

        @Test
        @DisplayName("재접속 실패 - 팀 멤버 아님")
        void handleReload_Fail_NotTeamMember() {
            // given
            Long roomId = 1L;
            Long userId = 999L;
            User user = createMockUser(userId, "OutsiderUser");

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> leaderGameService.handleReload(roomId, user))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_TEAM_MEMBER));
        }

        @Test
        @DisplayName("재접속 시 RESULT 단계면 당첨자 정보 포함")
        void handleReload_WithWinnerInfo() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            Long winnerId = 2L;
            User user = createMockUser(userId, "TestUser");
            TeamMember teamMember1 = createMockTeamMember(1L, roomId, userId, TeamRole.MEMBER);
            TeamMember teamMember2 = createMockTeamMember(2L, roomId, winnerId, TeamRole.MEMBER);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.of(teamMember1));
            given(teamMemberRepository.findByTeamRoomId(roomId)).willReturn(List.of(teamMember1, teamMember2));
            given(redisService.getConnectedUsers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getPhase(roomId)).willReturn(GamePhase.RESULT);
            given(redisService.getPhaseStartTime(roomId)).willReturn(System.currentTimeMillis());
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getVotedUsers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getSelectedGame(roomId)).willReturn(GameType.LADDER);
            given(redisService.getWinner(roomId)).willReturn(winnerId);

            // when
            ReloadPayload result = leaderGameService.handleReload(roomId, user);

            // then
            assertThat(result).isNotNull();
            assertThat(result.currentPhase()).isEqualTo(GamePhase.RESULT);
            assertThat(result.winnerId()).isEqualTo(winnerId);
            assertThat(result.selectedGame()).isEqualTo(GameType.LADDER);
        }
    }

    // ==================== 퇴장 테스트 ====================

    @Nested
    @DisplayName("handleLeave - 게임 방 퇴장")
    class HandleLeaveTest {

        @Test
        @DisplayName("퇴장 성공")
        void handleLeave_Success() {
            // given
            Long roomId = 1L;
            Long userId = 1L;

            // when
            leaderGameService.handleLeave(roomId, userId);

            // then
            verify(redisService).removeConnection(roomId, userId);
            verify(messagingTemplate).convertAndSend(eq("/sub/room/" + roomId), any(Object.class));
        }
    }

    // ==================== 지원 단계 시작 테스트 ====================

    @Nested
    @DisplayName("startVolunteerPhase - 지원 단계 시작")
    class StartVolunteerPhaseTest {

        @Test
        @DisplayName("지원 단계 시작 성공")
        void startVolunteerPhase_Success() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            TeamRoom teamRoom = createMockTeamRoom(roomId, Workflow.PENDING);
            TeamMember host = createMockTeamMember(1L, roomId, hostUserId, TeamRole.HOST);
            TeamMember member2 = createMockTeamMember(2L, roomId, 2L, TeamRole.MEMBER);

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, hostUserId))
                    .willReturn(Optional.of(host));
            given(redisService.isGameExists(roomId)).willReturn(false);
            given(teamMemberRepository.findByTeamRoomId(roomId)).willReturn(List.of(host, member2));
            given(redisService.areAllMembersConnected(eq(roomId), anyList())).willReturn(true);

            // when
            VolunteerPhaseResponse result = leaderGameService.startVolunteerPhase(roomId, hostUserId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.durationSeconds()).isEqualTo(60000);
            verify(dbService).startLeaderSelection(roomId);
            verify(redisService).initializeGame(eq(roomId), anyList());
            verify(messagingTemplate).convertAndSend(eq("/sub/room/" + roomId), any(Object.class));
            verify(taskScheduler).schedule(any(Runnable.class), any(java.time.Instant.class));
        }

        @Test
        @DisplayName("지원 단계 시작 실패 - 방장 아님")
        void startVolunteerPhase_Fail_NotHost() {
            // given
            Long roomId = 1L;
            Long userId = 2L;
            TeamRoom teamRoom = createMockTeamRoom(roomId, Workflow.PENDING);
            TeamMember member = createMockTeamMember(2L, roomId, userId, TeamRole.MEMBER);

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.of(member));

            // when / then
            assertThatThrownBy(() -> leaderGameService.startVolunteerPhase(roomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_ROOM_HOST));
        }

        @Test
        @DisplayName("지원 단계 시작 실패 - 이미 게임 진행 중")
        void startVolunteerPhase_Fail_GameAlreadyInProgress() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            TeamRoom teamRoom = createMockTeamRoom(roomId, Workflow.LEADER_SELECTION);

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));

            // when / then
            assertThatThrownBy(() -> leaderGameService.startVolunteerPhase(roomId, hostUserId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GAME_ALREADY_IN_PROGRESS));
        }

        @Test
        @DisplayName("지원 단계 시작 실패 - Redis에 게임 존재")
        void startVolunteerPhase_Fail_GameExistsInRedis() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            TeamRoom teamRoom = createMockTeamRoom(roomId, Workflow.PENDING);
            TeamMember host = createMockTeamMember(1L, roomId, hostUserId, TeamRole.HOST);

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, hostUserId))
                    .willReturn(Optional.of(host));
            given(redisService.isGameExists(roomId)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> leaderGameService.startVolunteerPhase(roomId, hostUserId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GAME_ALREADY_IN_PROGRESS));
        }

        @Test
        @DisplayName("지원 단계 시작 실패 - 전원 미접속")
        void startVolunteerPhase_Fail_NotAllMembersConnected() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            TeamRoom teamRoom = createMockTeamRoom(roomId, Workflow.PENDING);
            TeamMember host = createMockTeamMember(1L, roomId, hostUserId, TeamRole.HOST);
            TeamMember member2 = createMockTeamMember(2L, roomId, 2L, TeamRole.MEMBER);

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, hostUserId))
                    .willReturn(Optional.of(host));
            given(redisService.isGameExists(roomId)).willReturn(false);
            given(teamMemberRepository.findByTeamRoomId(roomId)).willReturn(List.of(host, member2));
            given(redisService.areAllMembersConnected(eq(roomId), anyList())).willReturn(false);

            // when / then
            assertThatThrownBy(() -> leaderGameService.startVolunteerPhase(roomId, hostUserId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_ALL_MEMBERS_CONNECTED));
        }

        @Test
        @DisplayName("지원 단계 시작 실패 - 팀 멤버 아님")
        void startVolunteerPhase_Fail_NotTeamMember() {
            // given
            Long roomId = 1L;
            Long userId = 999L;
            TeamRoom teamRoom = createMockTeamRoom(roomId, Workflow.PENDING);

            given(teamRoomRepository.findById(roomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> leaderGameService.startVolunteerPhase(roomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_TEAM_MEMBER));
        }
    }

    // ==================== 팀장 지원 테스트 ====================

    @Nested
    @DisplayName("vote - 팀장 지원/패스")
    class VoteTest {

        @Test
        @DisplayName("팀장 지원 성공")
        void vote_Volunteer_Success() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            TeamMember member = createMockTeamMember(1L, roomId, userId, TeamRole.MEMBER);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.of(member));
            given(redisService.hasVoted(roomId, userId)).willReturn(false);
            given(redisService.getParticipants(roomId)).willReturn(createParticipants(3));
            given(redisService.getVotedUsers(roomId)).willReturn(Set.of(userId));
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(userId));

            // when
            leaderGameService.vote(roomId, userId, true);

            // then
            verify(redisService).addVotedUser(roomId, userId);
            verify(redisService).addVolunteer(roomId, userId);
        }

        @Test
        @DisplayName("팀장 패스 성공")
        void vote_Pass_Success() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            TeamMember member = createMockTeamMember(1L, roomId, userId, TeamRole.MEMBER);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.of(member));
            given(redisService.hasVoted(roomId, userId)).willReturn(false);
            given(redisService.getParticipants(roomId)).willReturn(createParticipants(3));
            given(redisService.getVotedUsers(roomId)).willReturn(Set.of(userId));
            given(redisService.getVolunteers(roomId)).willReturn(new HashSet<>());

            // when
            leaderGameService.vote(roomId, userId, false);

            // then
            verify(redisService).addVotedUser(roomId, userId);
            verify(redisService, never()).addVolunteer(roomId, userId);
        }

        @Test
        @DisplayName("투표 실패 - 잘못된 게임 단계")
        void vote_Fail_InvalidPhase() {
            // given
            Long roomId = 1L;
            Long userId = 1L;

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_SELECT);

            // when / then
            assertThatThrownBy(() -> leaderGameService.vote(roomId, userId, true))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_GAME_PHASE));
        }

        @Test
        @DisplayName("투표 실패 - 이미 투표함")
        void vote_Fail_AlreadyVoted() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            TeamMember member = createMockTeamMember(1L, roomId, userId, TeamRole.MEMBER);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.of(member));
            given(redisService.hasVoted(roomId, userId)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> leaderGameService.vote(roomId, userId, true))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_VOLUNTEER));
        }

        @Test
        @DisplayName("투표 실패 - 팀 멤버 아님")
        void vote_Fail_NotTeamMember() {
            // given
            Long roomId = 1L;
            Long userId = 999L;

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> leaderGameService.vote(roomId, userId, true))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_TEAM_MEMBER));
        }
    }

    // ==================== 지원 단계 종료 테스트 ====================

    @Nested
    @DisplayName("endVolunteerPhase - 지원 단계 종료")
    class EndVolunteerPhaseTest {

        @Test
        @DisplayName("지원자 1명일 때 바로 팀장 확정 및 알림 발행")
        void endVolunteerPhase_SingleVolunteer_FinishGame() {
            // given
            Long roomId = 1L;
            Long winnerId = 1L;
            List<GameParticipant> participants = createParticipants(3);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(winnerId));
            given(redisService.getParticipants(roomId)).willReturn(participants);

            // when - private 메서드 호출
            ReflectionTestUtils.invokeMethod(leaderGameService, "endVolunteerPhase", roomId);

            // then
            verify(redisService).setPhase(roomId, GamePhase.RESULT);
            verify(redisService).setWinner(roomId, winnerId);
            verify(dbService).applyLeaderResult(roomId, winnerId);
            verify(messagingTemplate).convertAndSend(eq("/sub/room/" + roomId), any(Object.class));
            verify(eventPublisher).publishEvent(argThat((NotificationEvent event) ->
                    event.teamRoomId().equals(roomId) &&
                    event.targetId().equals(winnerId) &&
                    event.type() == NotificationType.TEAM_LEADER_CONFIRM
            ));
        }

        @Test
        @DisplayName("지원자 없을 때 전원을 지원자로 추가 후 GAME_SELECT 단계로")
        void endVolunteerPhase_NoVolunteers_AddAllAsVolunteers() {
            // given
            Long roomId = 1L;
            List<GameParticipant> participants = createParticipants(3);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(redisService.getVolunteers(roomId)).willReturn(new HashSet<>());
            given(redisService.getParticipants(roomId)).willReturn(participants);

            // when
            ReflectionTestUtils.invokeMethod(leaderGameService, "endVolunteerPhase", roomId);

            // then
            verify(redisService, times(3)).addVolunteer(eq(roomId), anyLong());
            verify(redisService).setPhase(roomId, GamePhase.GAME_SELECT);
            verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("지원자 2명 이상일 때 GAME_SELECT 단계로 전환")
        void endVolunteerPhase_MultipleVolunteers_GoToGameSelect() {
            // given
            Long roomId = 1L;
            List<GameParticipant> participants = createParticipants(3);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getParticipants(roomId)).willReturn(participants);

            // when
            ReflectionTestUtils.invokeMethod(leaderGameService, "endVolunteerPhase", roomId);

            // then
            verify(redisService).setPhase(roomId, GamePhase.GAME_SELECT);
            verify(dbService, never()).applyLeaderResult(anyLong(), anyLong());
            verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
        }
    }

    // ==================== 게임 선택 테스트 ====================

    @Nested
    @DisplayName("selectGame - 게임 선택")
    class SelectGameTest {

        @Test
        @DisplayName("사다리 게임 선택 성공")
        void selectGame_Ladder_Success() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            Long winnerId = 1L;
            TeamMember host = createMockTeamMember(1L, roomId, hostUserId, TeamRole.HOST);
            List<GameParticipant> participants = createParticipants(3);
            GameResultPayload mockResult = GameResultPayload.ladder(winnerId, "User1", participants, null);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_SELECT);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, hostUserId))
                    .willReturn(Optional.of(host));
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getParticipants(roomId)).willReturn(participants);
            given(ladderGameService.play(anyList())).willReturn(mockResult);

            // when
            leaderGameService.selectGame(roomId, hostUserId, GameType.LADDER);

            // then
            verify(redisService).setSelectedGame(roomId, GameType.LADDER);
            verify(ladderGameService).play(anyList());
            verify(dbService).applyLeaderResult(roomId, winnerId);
            verify(messagingTemplate).convertAndSend(eq("/sub/room/" + roomId), any(Object.class));
            verify(eventPublisher).publishEvent(argThat((NotificationEvent event) ->
                    event.teamRoomId().equals(roomId) &&
                    event.targetId().equals(winnerId) &&
                    event.type() == NotificationType.TEAM_LEADER_CONFIRM
            ));
        }

        @Test
        @DisplayName("룰렛 게임 선택 성공")
        void selectGame_Roulette_Success() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            Long winnerId = 2L;
            TeamMember host = createMockTeamMember(1L, roomId, hostUserId, TeamRole.HOST);
            List<GameParticipant> participants = createParticipants(3);
            GameResultPayload mockResult = GameResultPayload.roulette(winnerId, "User2", participants, 1);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_SELECT);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, hostUserId))
                    .willReturn(Optional.of(host));
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L, 2L, 3L));
            given(redisService.getParticipants(roomId)).willReturn(participants);
            given(rouletteGameService.play(anyList())).willReturn(mockResult);

            // when
            leaderGameService.selectGame(roomId, hostUserId, GameType.ROULETTE);

            // then
            verify(redisService).setSelectedGame(roomId, GameType.ROULETTE);
            verify(rouletteGameService).play(anyList());
            verify(dbService).applyLeaderResult(roomId, winnerId);
            verify(eventPublisher).publishEvent(argThat((NotificationEvent event) ->
                    event.teamRoomId().equals(roomId) &&
                    event.targetId().equals(winnerId) &&
                    event.type() == NotificationType.TEAM_LEADER_CONFIRM
            ));
        }

        @Test
        @DisplayName("타이밍 게임 선택 성공 - GAME_PLAYING 전환")
        void selectGame_Timing_Success() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            TeamMember host = createMockTeamMember(1L, roomId, hostUserId, TeamRole.HOST);
            List<GameParticipant> participants = createParticipants(3);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_SELECT);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, hostUserId))
                    .willReturn(Optional.of(host));
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getParticipants(roomId)).willReturn(participants);

            // when
            leaderGameService.selectGame(roomId, hostUserId, GameType.TIMING);

            // then
            verify(redisService).setSelectedGame(roomId, GameType.TIMING);
            verify(redisService).setPhase(roomId, GamePhase.GAME_PLAYING);
            verify(messagingTemplate).convertAndSend(eq("/sub/room/" + roomId), any(Object.class));
            verify(ladderGameService, never()).play(anyList());
            verify(rouletteGameService, never()).play(anyList());
        }

        @Test
        @DisplayName("게임 선택 실패 - 잘못된 게임 단계")
        void selectGame_Fail_InvalidPhase() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;

            given(redisService.getPhase(roomId)).willReturn(GamePhase.VOLUNTEER);

            // when / then
            assertThatThrownBy(() -> leaderGameService.selectGame(roomId, hostUserId, GameType.LADDER))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_GAME_PHASE));
        }

        @Test
        @DisplayName("게임 선택 실패 - 방장 아님")
        void selectGame_Fail_NotHost() {
            // given
            Long roomId = 1L;
            Long userId = 2L;
            TeamMember member = createMockTeamMember(2L, roomId, userId, TeamRole.MEMBER);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_SELECT);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, userId))
                    .willReturn(Optional.of(member));

            // when / then
            assertThatThrownBy(() -> leaderGameService.selectGame(roomId, userId, GameType.LADDER))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_ROOM_HOST));
        }

        @Test
        @DisplayName("지원자 없을 때 전체 참가자로 게임 진행")
        void selectGame_NoVolunteers_UseAllParticipants() {
            // given
            Long roomId = 1L;
            Long hostUserId = 1L;
            TeamMember host = createMockTeamMember(1L, roomId, hostUserId, TeamRole.HOST);
            List<GameParticipant> participants = createParticipants(3);
            GameResultPayload mockResult = GameResultPayload.ladder(1L, "User1", participants, null);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_SELECT);
            given(teamMemberRepository.findByTeamRoomIdAndUserId(roomId, hostUserId))
                    .willReturn(Optional.of(host));
            given(redisService.getVolunteers(roomId)).willReturn(new HashSet<>());
            given(redisService.getParticipants(roomId)).willReturn(participants);
            given(ladderGameService.play(anyList())).willReturn(mockResult);

            // when
            leaderGameService.selectGame(roomId, hostUserId, GameType.LADDER);

            // then
            verify(ladderGameService).play(argThat(list -> list.size() == 3));
        }
    }

    // ==================== 타이밍 게임 결과 제출 테스트 ====================

    @Nested
    @DisplayName("submitTimingResult - 타이밍 게임 결과 제출")
    class SubmitTimingResultTest {

        @Test
        @DisplayName("타이밍 결과 제출 성공")
        void submitTimingResult_Success() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            Double timeDifference = 1.234;

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_PLAYING);
            given(redisService.getSelectedGame(roomId)).willReturn(GameType.TIMING);
            given(redisService.hasTimingRecord(roomId, userId)).willReturn(false);
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getParticipants(roomId)).willReturn(createParticipants(2));
            given(redisService.getTimingRecordCount(roomId)).willReturn(1L);

            // when
            leaderGameService.submitTimingResult(roomId, userId, timeDifference);

            // then
            verify(redisService).recordTimingResult(roomId, userId, timeDifference);
        }

        @Test
        @DisplayName("타이밍 결과 제출 - 전원 완료 시 게임 종료")
        void submitTimingResult_AllCompleted_FinishGame() {
            // given
            Long roomId = 1L;
            Long userId = 2L;
            Long winnerId = 1L;
            Double timeDifference = 0.5;
            List<GameParticipant> participants = createParticipants(2);

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_PLAYING);
            given(redisService.getSelectedGame(roomId)).willReturn(GameType.TIMING);
            given(redisService.hasTimingRecord(roomId, userId)).willReturn(false);
            given(redisService.getVolunteers(roomId)).willReturn(Set.of(1L, 2L));
            given(redisService.getParticipants(roomId)).willReturn(participants);
            given(redisService.getTimingRecordCount(roomId)).willReturn(2L);
            given(redisService.getMaxDifferenceUserId(roomId)).willReturn(winnerId);

            // when
            leaderGameService.submitTimingResult(roomId, userId, timeDifference);

            // then
            verify(redisService).recordTimingResult(roomId, userId, timeDifference);
            verify(redisService).setPhase(roomId, GamePhase.RESULT);
            verify(dbService).applyLeaderResult(roomId, winnerId);
            verify(eventPublisher).publishEvent(argThat((NotificationEvent event) ->
                    event.teamRoomId().equals(roomId) &&
                    event.targetId().equals(winnerId) &&
                    event.type() == NotificationType.TEAM_LEADER_CONFIRM
            ));
        }

        @Test
        @DisplayName("타이밍 결과 제출 실패 - 잘못된 게임 단계")
        void submitTimingResult_Fail_InvalidPhase() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            Double timeDifference = 1.0;

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_SELECT);

            // when / then
            assertThatThrownBy(() -> leaderGameService.submitTimingResult(roomId, userId, timeDifference))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_GAME_PHASE));
        }

        @Test
        @DisplayName("타이밍 결과 제출 실패 - 타이밍 게임 아님")
        void submitTimingResult_Fail_NotTimingGame() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            Double timeDifference = 1.0;

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_PLAYING);
            given(redisService.getSelectedGame(roomId)).willReturn(GameType.LADDER);

            // when / then
            assertThatThrownBy(() -> leaderGameService.submitTimingResult(roomId, userId, timeDifference))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_GAME_PHASE));
        }

        @Test
        @DisplayName("타이밍 결과 제출 실패 - 이미 제출함")
        void submitTimingResult_Fail_AlreadyStopped() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            Double timeDifference = 1.0;

            given(redisService.getPhase(roomId)).willReturn(GamePhase.GAME_PLAYING);
            given(redisService.getSelectedGame(roomId)).willReturn(GameType.TIMING);
            given(redisService.hasTimingRecord(roomId, userId)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> leaderGameService.submitTimingResult(roomId, userId, timeDifference))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode())
                            .isEqualTo(ErrorCode.TIMING_ALREADY_STOPPED));
        }
    }

    // ==================== 결과 확인 테스트 ====================

    @Nested
    @DisplayName("confirmResult - 결과 확인")
    class ConfirmResultTest {

        @Test
        @DisplayName("결과 확인 - 다른 참가자 남아있음")
        void confirmResult_OthersRemaining() {
            // given
            Long roomId = 1L;
            Long userId = 1L;

            given(redisService.getConnectedUsers(roomId)).willReturn(Set.of(2L, 3L));

            // when
            leaderGameService.confirmResult(roomId, userId);

            // then
            verify(redisService).removeConnection(roomId, userId);
            verify(redisService, never()).deleteGame(roomId);
        }

        @Test
        @DisplayName("결과 확인 - 마지막 참가자, 게임 데이터 삭제")
        void confirmResult_LastParticipant_DeleteGame() {
            // given
            Long roomId = 1L;
            Long userId = 1L;

            given(redisService.getConnectedUsers(roomId)).willReturn(new HashSet<>());

            // when
            leaderGameService.confirmResult(roomId, userId);

            // then
            verify(redisService).removeConnection(roomId, userId);
            verify(redisService).deleteGame(roomId);
            verify(redisService).deleteTimingRecords(roomId);
        }
    }
}
