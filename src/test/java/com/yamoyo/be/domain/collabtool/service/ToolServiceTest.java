package com.yamoyo.be.domain.collabtool.service;

import com.yamoyo.be.domain.collabtool.dto.request.ToolVoteRequest;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteCountResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteParticipationResponse;
import com.yamoyo.be.domain.collabtool.repository.MemberToolVoteRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolService 단위 테스트")
class ToolServiceTest {

    @InjectMocks
    private ToolService toolService;

    @Mock
    private MemberToolVoteRepository toolVoteRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Nested
    @DisplayName("협업툴 투표 일괄 제출")
    class SubmitAllToolVotes {

        @Test
        @DisplayName("성공: 첫 투표 제출")
        void submitAllToolVotes_Success_FirstVote() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            ToolVoteRequest request = new ToolVoteRequest(List.of(
                    new ToolVoteRequest.CategoryToolVote(1, List.of(1, 2)),
                    new ToolVoteRequest.CategoryToolVote(2, List.of(3))
            ));

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.getId()).willReturn(1L);
            given(toolVoteRepository.existsByMemberId(1L)).willReturn(false);

            // when
            toolService.submitAllToolVotes(teamRoomId, userId, request);

            // then
            then(toolVoteRepository).should().saveAll(any(List.class));
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void submitAllToolVotes_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            ToolVoteRequest request = new ToolVoteRequest(List.of(
                    new ToolVoteRequest.CategoryToolVote(1, List.of(1, 2))
            ));

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.submitAllToolVotes(teamRoomId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 팀원이 아님")
        void submitAllToolVotes_Fail_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            ToolVoteRequest request = new ToolVoteRequest(List.of(
                    new ToolVoteRequest.CategoryToolVote(1, List.of(1, 2))
            ));

            TeamRoom teamRoom = mock(TeamRoom.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.submitAllToolVotes(teamRoomId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MEMBER);
        }

        @Test
        @DisplayName("실패: 이미 투표함")
        void submitAllToolVotes_Fail_AlreadyVoted() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            ToolVoteRequest request = new ToolVoteRequest(List.of(
                    new ToolVoteRequest.CategoryToolVote(1, List.of(1, 2))
            ));

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.getId()).willReturn(1L);
            given(toolVoteRepository.existsByMemberId(1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> toolService.submitAllToolVotes(teamRoomId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_VOTED);
        }
    }

    @Nested
    @DisplayName("카테고리별 득표 현황 조회")
    class GetVoteCountByCategory {

        @Test
        @DisplayName("성공: 카테고리별 득표 현황 조회")
        void getVoteCountByCategory_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Integer categoryId = 1;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(toolVoteRepository.countVotesByCategory(teamRoomId, categoryId))
                    .willReturn(List.of(
                            new Object[]{1, 5L},
                            new Object[]{2, 3L}
                    ));

            // when
            ToolVoteCountResponse response = toolService.getVoteCountByCategory(teamRoomId, userId, categoryId);

            // then
            assertThat(response.categoryId()).isEqualTo(categoryId);
            assertThat(response.tools()).hasSize(2);
            assertThat(response.tools().get(0).toolId()).isEqualTo(1);
            assertThat(response.tools().get(0).voteCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void getVoteCountByCategory_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Integer categoryId = 1;

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getVoteCountByCategory(teamRoomId, userId, categoryId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 팀원이 아님")
        void getVoteCountByCategory_Fail_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Integer categoryId = 1;

            TeamRoom teamRoom = mock(TeamRoom.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getVoteCountByCategory(teamRoomId, userId, categoryId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MEMBER);
        }
    }

    @Nested
    @DisplayName("투표 참여 현황 조회")
    class GetVotedMemberParticipation {

        @Test
        @DisplayName("성공: 투표 참여 현황 조회")
        void getVotedMemberParticipation_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember requestMember = mock(TeamMember.class);

            User votedUser = mock(User.class);
            TeamMember votedMember = mock(TeamMember.class);

            User notVotedUser = mock(User.class);
            TeamMember notVotedMember = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(requestMember));
            given(teamMemberRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(List.of(votedMember, notVotedMember));
            given(toolVoteRepository.findVotedMemberIds(teamRoomId))
                    .willReturn(List.of(1L));

            given(votedMember.getId()).willReturn(1L);
            given(votedMember.getUser()).willReturn(votedUser);
            given(votedUser.getId()).willReturn(1L);
            given(votedUser.getName()).willReturn("투표한사람");
            given(votedUser.getProfileImageId()).willReturn(1L);

            given(notVotedMember.getId()).willReturn(2L);
            given(notVotedMember.getUser()).willReturn(notVotedUser);
            given(notVotedUser.getId()).willReturn(2L);
            given(notVotedUser.getName()).willReturn("안한사람");
            given(notVotedUser.getProfileImageId()).willReturn(2L);

            // when
            ToolVoteParticipationResponse response = toolService.getVotedMemberParticipation(teamRoomId, userId);

            // then
            assertThat(response.totalMembers()).isEqualTo(2);
            assertThat(response.votedMembers()).isEqualTo(1);
            assertThat(response.voted()).hasSize(1);
            assertThat(response.notVoted()).hasSize(1);
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void getVotedMemberParticipation_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getVotedMemberParticipation(teamRoomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 팀원이 아님")
        void getVotedMemberParticipation_Fail_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getVotedMemberParticipation(teamRoomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MEMBER);
        }
    }
}