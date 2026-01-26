package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.request.JoinTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.CreateTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.dto.response.JoinTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamRoomServiceTest {

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InviteTokenService inviteTokenService;

    @InjectMocks
    private TeamRoomService teamRoomService;

    @Test
    void 팀룸_생성() {
        // given
        Long userId = 1L;
        CreateTeamRoomRequest request = new CreateTeamRoomRequest(
                "팀룸 제목",
                "팀룸 설명",
                LocalDateTime.now().plusDays(7),
                null
        );

        User user = User.create("test@example.com", "테스트유저");

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
    void 신규멤버_팀룸_입장() {
        // given
        Long userId = 1L;
        String token = "test-token";
        Long teamRoomId = 10L;

        JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);

        User user = User.create("test@example.com", "테스트유저");

        TeamRoom teamRoom = TeamRoom.builder()
                .title("팀룸 제목")
                .description("팀룸 설명")
                .deadline(LocalDateTime.now().plusDays(7))
                .build();
        // TeamRoom이 builder로 lifecycle/workflow 기본값을 설정 안 한다면 아래처럼 세팅 필요할 수 있음
        // teamRoom.setLifecycle(Lifecycle.ACTIVE);
        // teamRoom.setWorkflow(Workflow.PROGRESS); // 허용되는 workflow로

        given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
        given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
        given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)).willReturn(Optional.empty());
        given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(1L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // save 시 id가 필요하면 mock으로 id 세팅된 객체를 반환
        TeamMember saved = TeamMember.createMember(teamRoom, user);
        given(teamMemberRepository.save(any(TeamMember.class))).willReturn(saved);

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
    }


    @Test
    void 기존멤버_팀룸_입장() {
        // given
        Long userId = 1L;
        String token = "test-token";
        Long teamRoomId = 10L;

        JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);

        TeamRoom teamRoom = TeamRoom.builder()
                .title("팀룸 제목")
                .description("팀룸 설명")
                .deadline(LocalDateTime.now().plusDays(7))
                .build();

        TeamMember existing = mock(TeamMember.class);
        given(existing.getId()).willReturn(99L);

        given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
        given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
        given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)).willReturn(Optional.of(existing));

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
    void 팀룸_입장_정원초과() {
        // given
        Long userId = 1L;
        String token = "test-token";
        Long teamRoomId = 10L;

        JoinTeamRoomRequest request = new JoinTeamRoomRequest(token);

        TeamRoom teamRoom = TeamRoom.builder()
                .title("팀룸 제목")
                .description("팀룸 설명")
                .deadline(LocalDateTime.now().plusDays(7))
                .build();

        given(inviteTokenService.getTeamRoomIdByToken(token)).willReturn(teamRoomId);
        given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
        given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId)).willReturn(Optional.empty());
        given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(12L);

        // when / then
        assertThatThrownBy(() -> teamRoomService.joinTeamRoom(request, userId))
                .isInstanceOf(YamoyoException.class)
                .hasMessageContaining(ErrorCode.TEAMROOM_FULL.getMessage());

        verify(teamMemberRepository, never()).save(any());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void 팀룸_입장_초대토큰_유효X() {
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

}