package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.teamroom.dto.request.CreateTeamRoomRequest;
import com.yamoyo.be.domain.teamroom.dto.response.CreateTeamRoomResponse;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.verify;

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

}