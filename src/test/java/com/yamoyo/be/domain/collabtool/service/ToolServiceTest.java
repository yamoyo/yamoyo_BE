package com.yamoyo.be.domain.collabtool.service;

import com.yamoyo.be.domain.collabtool.dto.request.ApproveProposalRequest;
import com.yamoyo.be.domain.collabtool.dto.request.ProposeToolRequest;
import com.yamoyo.be.domain.collabtool.dto.request.ToolVoteRequest;
import com.yamoyo.be.domain.collabtool.dto.response.ProposalDetailResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteCountResponse;
import com.yamoyo.be.domain.collabtool.dto.response.ToolVoteParticipationResponse;
import com.yamoyo.be.domain.collabtool.entity.ToolProposal;
import com.yamoyo.be.domain.collabtool.repository.MemberToolVoteRepository;
import com.yamoyo.be.domain.collabtool.repository.TeamToolRepository;
import com.yamoyo.be.domain.collabtool.repository.ToolProposalRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
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

    @Mock
    private ToolProposalRepository toolProposalRepository;

    @Mock
    private TeamToolRepository teamToolRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRoomSetupRepository setupRepository;

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
            TeamRoomSetup setup = mock(TeamRoomSetup.class);
            given(setupRepository.findByTeamRoomId(teamRoomId)).willReturn(Optional.of(setup));

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.getId()).willReturn(1L);
            given(toolVoteRepository.existsByMemberId(1L)).willReturn(false);

            // when
            toolService.submitAllToolVotes(teamRoomId, userId, request);

            // then
            then(toolVoteRepository).should().saveAll(anyList());
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

    @Nested
    @DisplayName("협업툴 제안")
    class ProposeTool {

        @Test
        @DisplayName("성공: 협업툴 제안")
        void proposeTool_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            ProposeToolRequest request = new ProposeToolRequest(1, 5);

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            ToolProposal proposal = mock(ToolProposal.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(toolProposalRepository.save(any(ToolProposal.class))).willReturn(proposal);

            // when
            toolService.proposeTool(teamRoomId, userId, request);

            // then
            then(toolProposalRepository).should().save(any(ToolProposal.class));
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void proposeTool_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            ProposeToolRequest request = new ProposeToolRequest(1, 5);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.proposeTool(teamRoomId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 팀원이 아님")
        void proposeTool_Fail_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            ProposeToolRequest request = new ProposeToolRequest(1, 5);

            TeamRoom teamRoom = mock(TeamRoom.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.proposeTool(teamRoomId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MEMBER);
        }
    }

    @Nested
    @DisplayName("제안 승인/반려")
    class ApproveOrRejectProposal {

        @Test
        @DisplayName("성공: 제안 승인 및 확정 테이블 추가")
        void approveProposal_Success() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            ApproveProposalRequest request = new ApproveProposalRequest(true);

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            ToolProposal proposal = mock(ToolProposal.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(toolProposalRepository.existsByIdAndTeamRoomId(proposalId, teamRoomId)).willReturn(true);
            given(proposal.getCategoryId()).willReturn(1);
            given(proposal.getToolId()).willReturn(5);

            // when
            toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request);

            // then
            then(proposal).should().updateApprovalStatus(true);
            then(teamToolRepository).should().save(any());
        }

        @Test
        @DisplayName("성공: 제안 반려")
        void rejectProposal_Success() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            ApproveProposalRequest request = new ApproveProposalRequest(false);

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            ToolProposal proposal = mock(ToolProposal.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(toolProposalRepository.existsByIdAndTeamRoomId(proposalId, teamRoomId)).willReturn(true);

            // when
            toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request);

            // then
            then(proposal).should().updateApprovalStatus(false);
            then(teamToolRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void approveOrRejectProposal_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            ApproveProposalRequest request = new ApproveProposalRequest(true);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 팀원이 아님")
        void approveOrRejectProposal_Fail_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            ApproveProposalRequest request = new ApproveProposalRequest(true);

            TeamRoom teamRoom = mock(TeamRoom.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MEMBER);
        }

        @Test
        @DisplayName("실패: 팀장 권한이 없음")
        void approveOrRejectProposal_Fail_NotTeamManager() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            ApproveProposalRequest request = new ApproveProposalRequest(true);

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(false);

            // when & then
            assertThatThrownBy(() -> toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MANAGER);
        }

        @Test
        @DisplayName("실패: 제안을 찾을 수 없음")
        void approveOrRejectProposal_Fail_ProposalNotFound() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            ApproveProposalRequest request = new ApproveProposalRequest(true);

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROPOSAL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 해당 팀룸의 제안이 아님")
        void approveOrRejectProposal_Fail_ProposalNotBelongsToTeamRoom() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            ApproveProposalRequest request = new ApproveProposalRequest(true);

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            ToolProposal proposal = mock(ToolProposal.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(toolProposalRepository.existsByIdAndTeamRoomId(proposalId, teamRoomId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> toolService.approveOrRejectProposal(teamRoomId, proposalId, userId, request))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROPOSAL_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("제안 상세 조회")
    class GetProposalDetail {

        @Test
        @DisplayName("성공: 제안 상세 조회")
        void getProposalDetail_Success() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            Long proposerId = 2L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            ToolProposal proposal = mock(ToolProposal.class);
            User proposer = mock(User.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(toolProposalRepository.existsByIdAndTeamRoomId(proposalId, teamRoomId)).willReturn(true);
            given(proposal.getRequestedBy()).willReturn(proposerId);
            given(userRepository.findById(proposerId)).willReturn(Optional.of(proposer));

            // when
            ProposalDetailResponse response = toolService.getProposalDetail(teamRoomId, proposalId, userId);

            // then
            assertThat(response).isNotNull();
            then(userRepository).should().findById(proposerId);
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void getProposalDetail_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getProposalDetail(teamRoomId, proposalId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 팀원이 아님")
        void getProposalDetail_Fail_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getProposalDetail(teamRoomId, proposalId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MEMBER);
        }

        @Test
        @DisplayName("실패: 팀장 권한이 없음")
        void getProposalDetail_Fail_NotTeamManager() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(false);

            // when & then
            assertThatThrownBy(() -> toolService.getProposalDetail(teamRoomId, proposalId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MANAGER);
        }

        @Test
        @DisplayName("실패: 제안을 찾을 수 없음")
        void getProposalDetail_Fail_ProposalNotFound() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getProposalDetail(teamRoomId, proposalId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROPOSAL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 해당 팀룸의 제안이 아님")
        void getProposalDetail_Fail_ProposalNotBelongsToTeamRoom() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            ToolProposal proposal = mock(ToolProposal.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(toolProposalRepository.existsByIdAndTeamRoomId(proposalId, teamRoomId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> toolService.getProposalDetail(teamRoomId, proposalId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROPOSAL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 제안자를 찾을 수 없음")
        void getProposalDetail_Fail_ProposerNotFound() {
            // given
            Long teamRoomId = 1L;
            Long proposalId = 1L;
            Long userId = 1L;
            Long proposerId = 2L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            ToolProposal proposal = mock(ToolProposal.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(true);
            given(toolProposalRepository.findById(proposalId)).willReturn(Optional.of(proposal));
            given(toolProposalRepository.existsByIdAndTeamRoomId(proposalId, teamRoomId)).willReturn(true);
            given(proposal.getRequestedBy()).willReturn(proposerId);
            given(userRepository.findById(proposerId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> toolService.getProposalDetail(teamRoomId, proposalId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }
}