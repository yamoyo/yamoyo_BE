package com.yamoyo.be.domain.rule.service;

import com.yamoyo.be.domain.rule.dto.request.RuleVoteRequest;
import com.yamoyo.be.domain.rule.dto.request.TeamRuleRequest;
import com.yamoyo.be.domain.rule.dto.response.RuleVoteParticipationResponse;
import com.yamoyo.be.domain.rule.dto.response.TeamRulesResponse;
import com.yamoyo.be.domain.rule.entity.MemberRuleVote;
import com.yamoyo.be.domain.rule.entity.RuleTemplate;
import com.yamoyo.be.domain.rule.entity.TeamRule;
import com.yamoyo.be.domain.rule.repository.MemberRuleVoteRepository;
import com.yamoyo.be.domain.rule.repository.RuleTemplateRepository;
import com.yamoyo.be.domain.rule.repository.TeamRuleRepository;
import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.repository.TeamMemberRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.event.event.NotificationEvent;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleService 단위 테스트")
class RuleServiceTest {

    @InjectMocks
    private RuleService ruleService;

    @Mock
    private RuleTemplateRepository ruleTemplateRepository;

    @Mock
    private MemberRuleVoteRepository memberRuleVoteRepository;

    @Mock
    private TeamRuleRepository teamRuleRepository;

    @Mock
    private TeamRoomRepository teamRoomRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamRoomSetupRepository setupRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("규칙 투표 제출")
    class SubmitRuleVote {

        @Test
        @DisplayName("성공: 첫 투표 제출")
        void submitRuleVote_Success_FirstVote() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Long ruleId = 1L;
            RuleVoteRequest request = new RuleVoteRequest(ruleId, true);

            TeamRoomSetup setup = mock(TeamRoomSetup.class);
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            RuleTemplate ruleTemplate = mock(RuleTemplate.class);

            // submitRuleVote 시작 시 setup 조회 및 체크
            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.getId()).willReturn(1L);
            given(ruleTemplateRepository.findById(ruleId)).willReturn(Optional.of(ruleTemplate));
            given(memberRuleVoteRepository.findByTeamRoomIdAndMemberIdAndRuleTemplateId(teamRoomId, 1L, ruleId))
                    .willReturn(Optional.empty());

            // confirmRules 호출 시 필요한 mock (전원 완료 아님)
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(5L);
            given(ruleTemplateRepository.count()).willReturn(10L);
            given(memberRuleVoteRepository.findMemberIdsWhoCompletedAllRules(teamRoomId, 10L))
                    .willReturn(List.of(1L)); // 1명만 완료

            // when
            ruleService.submitRuleVote(teamRoomId, request, userId);

            // then
            then(memberRuleVoteRepository).should().save(any(MemberRuleVote.class));
        }

        @Test
        @DisplayName("성공: 기존 투표 수정")
        void submitRuleVote_Success_UpdateVote() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Long ruleId = 1L;
            RuleVoteRequest request = new RuleVoteRequest(ruleId, false);

            TeamRoomSetup setup = mock(TeamRoomSetup.class);
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            RuleTemplate ruleTemplate = mock(RuleTemplate.class);
            MemberRuleVote existingVote = mock(MemberRuleVote.class);

            // submitRuleVote 시작 시 setup 조회 및 체크
            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.getId()).willReturn(1L);
            given(ruleTemplateRepository.findById(ruleId)).willReturn(Optional.of(ruleTemplate));
            given(memberRuleVoteRepository.findByTeamRoomIdAndMemberIdAndRuleTemplateId(teamRoomId, 1L, ruleId))
                    .willReturn(Optional.of(existingVote));
            given(existingVote.getId()).willReturn(1L);

            // confirmRules 호출 시 필요한 mock (전원 완료 아님)
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(5L);
            given(ruleTemplateRepository.count()).willReturn(10L);
            given(memberRuleVoteRepository.findMemberIdsWhoCompletedAllRules(teamRoomId, 10L))
                    .willReturn(List.of(1L));

            // when
            ruleService.submitRuleVote(teamRoomId, request, userId);

            // then
            then(existingVote).should().updateAgreement(false);
            then(memberRuleVoteRepository).should(never()).save(any(MemberRuleVote.class));
        }

        @Test
        @DisplayName("실패: TeamRoomSetup을 찾을 수 없음")
        void submitRuleVote_Fail_SetupNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            RuleVoteRequest request = new RuleVoteRequest(1L, true);

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ruleService.submitRuleVote(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETUP_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 확정된 경우 투표 처리 안함")
        void submitRuleVote_AlreadyCompleted() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            RuleVoteRequest request = new RuleVoteRequest(1L, true);

            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(true);

            // when
            ruleService.submitRuleVote(teamRoomId, request, userId);

            // then
            then(teamRoomRepository).should(never()).findById(any());
            then(memberRuleVoteRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void submitRuleVote_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            RuleVoteRequest request = new RuleVoteRequest(1L, true);

            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ruleService.submitRuleVote(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 팀원이 아님")
        void submitRuleVote_Fail_NotTeamMember() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            RuleVoteRequest request = new RuleVoteRequest(1L, true);

            TeamRoomSetup setup = mock(TeamRoomSetup.class);
            TeamRoom teamRoom = mock(TeamRoom.class);

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ruleService.submitRuleVote(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MEMBER);
        }

        @Test
        @DisplayName("실패: 규칙을 찾을 수 없음")
        void submitRuleVote_Fail_RuleNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Long ruleId = 999L;
            RuleVoteRequest request = new RuleVoteRequest(ruleId, true);

            TeamRoomSetup setup = mock(TeamRoomSetup.class);
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(ruleTemplateRepository.findById(ruleId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ruleService.submitRuleVote(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RULE_NOT_FOUND);
        }

        @Test
        @DisplayName("성공: 전원 투표 완료 시 규칙 자동 확정")
        void submitRuleVote_Success_AutoConfirm() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            Long ruleId = 1L;
            RuleVoteRequest request = new RuleVoteRequest(ruleId, true);

            TeamRoomSetup setup = mock(TeamRoomSetup.class);
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            RuleTemplate ruleTemplate = mock(RuleTemplate.class);
            RuleTemplate ruleTemplate2 = mock(RuleTemplate.class);

            // submitRuleVote 시작 시 setup 조회 및 체크
            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.getId()).willReturn(1L);
            given(ruleTemplateRepository.findById(ruleId)).willReturn(Optional.of(ruleTemplate));
            given(memberRuleVoteRepository.findByTeamRoomIdAndMemberIdAndRuleTemplateId(teamRoomId, 1L, ruleId))
                    .willReturn(Optional.empty());

            // confirmRules 호출 시 - 전원 완료
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(5L);
            given(ruleTemplateRepository.count()).willReturn(10L);
            given(memberRuleVoteRepository.findMemberIdsWhoCompletedAllRules(teamRoomId, 10L))
                    .willReturn(List.of(1L, 2L, 3L, 4L, 5L)); // 전원 완료

            // 규칙별 동의 득표 수: 규칙1=4표 (과반수 달성)
            List<Object[]> agreeCountResults = Collections.singletonList(new Object[]{1L, 4L});
            given(memberRuleVoteRepository.countAgreeVotesByRuleForMembers(teamRoomId, List.of(1L, 2L, 3L, 4L, 5L)))
                    .willReturn(agreeCountResults);
            given(ruleTemplateRepository.findById(1L)).willReturn(Optional.of(ruleTemplate2));
            given(ruleTemplate2.getContent()).willReturn("규칙 내용");

            // when
            ruleService.submitRuleVote(teamRoomId, request, userId);

            // then
            then(memberRuleVoteRepository).should().save(any(MemberRuleVote.class));
            then(teamRuleRepository).should().save(any(TeamRule.class));
            then(setup).should().completeRuleSetup();
            then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
        }
    }

    @Nested
    @DisplayName("규칙 확정")
    class ConfirmRules {

        @Test
        @DisplayName("성공: 과반수 이상 득표한 규칙 확정")
        void confirmRules_Success() {
            // given
            Long teamRoomId = 1L;
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);
            RuleTemplate ruleTemplate1 = mock(RuleTemplate.class);

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(5L);
            given(ruleTemplateRepository.count()).willReturn(10L);
            given(memberRuleVoteRepository.findMemberIdsWhoCompletedAllRules(teamRoomId, 10L))
                    .willReturn(List.of(1L, 2L, 3L, 4L, 5L)); // 전원 완료

            // 규칙별 동의 득표 수: 규칙1=4표, 규칙2=2표
            // 과반수 기준: (5/2)+1=3표
            List<Object[]> agreeCountResults = Arrays.asList(
                    new Object[]{1L, 4L},  // 규칙1: 4표 (과반수 달성)
                    new Object[]{2L, 2L}   // 규칙2: 2표 (과반수 미달)
            );
            given(memberRuleVoteRepository.countAgreeVotesByRuleForMembers(
                    teamRoomId, List.of(1L, 2L, 3L, 4L, 5L)))
                    .willReturn(agreeCountResults);
            given(ruleTemplateRepository.findById(1L)).willReturn(Optional.of(ruleTemplate1));
            given(ruleTemplate1.getContent()).willReturn("규칙1 내용");

            // when
            ruleService.confirmRules(teamRoomId);

            // then
            then(teamRuleRepository).should(times(1)).save(any(TeamRule.class));
            then(setup).should().completeRuleSetup();
            then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("실패: TeamRoomSetup을 찾을 수 없음")
        void confirmRules_Fail_SetupNotFound() {
            // given
            Long teamRoomId = 1L;

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ruleService.confirmRules(teamRoomId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETUP_NOT_FOUND);
        }

        @Test
        @DisplayName("전원 완료 아닐 때 확정 안됨")
        void confirmRules_NotAllCompleted() {
            // given
            Long teamRoomId = 1L;
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findByTeamRoomIdForUpdate(teamRoomId))
                    .willReturn(Optional.of(setup));
            given(setup.isRuleCompleted()).willReturn(false);
            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(mock(TeamRoom.class)));
            given(teamMemberRepository.countByTeamRoomId(teamRoomId)).willReturn(5L);
            given(ruleTemplateRepository.count()).willReturn(10L);
            given(memberRuleVoteRepository.findMemberIdsWhoCompletedAllRules(teamRoomId, 10L))
                    .willReturn(List.of(1L, 2L)); // 2명만 완료

            // when
            ruleService.confirmRules(teamRoomId);

            // then
            then(teamRuleRepository).should(never()).save(any());
            then(setup).should(never()).completeRuleSetup();
        }
    }

    @Nested
    @DisplayName("규칙 투표 참여 현황 조회")
    class GetRuleVoteParticipation {

        @Test
        @DisplayName("성공: 참여 현황 조회")
        void getRuleVoteParticipation_Success() {
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

            // 투표 완료 판단: 모든 규칙(10개)에 투표 완료한 팀원만
            given(ruleTemplateRepository.count()).willReturn(10L);
            given(memberRuleVoteRepository.findMemberIdsWhoCompletedAllRules(teamRoomId, 10L))
                    .willReturn(List.of(1L)); // votedMember.getId()

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
            RuleVoteParticipationResponse response = ruleService.getRuleVoteParticipation(teamRoomId, userId);

            // then
            assertThat(response.totalMembers()).isEqualTo(2);
            assertThat(response.votedMembers()).isEqualTo(1);
            assertThat(response.voted()).hasSize(1);
            assertThat(response.notVoted()).hasSize(1);
        }

        @Test
        @DisplayName("실패: 팀룸을 찾을 수 없음")
        void getRuleVoteParticipation_Fail_TeamRoomNotFound() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ruleService.getRuleVoteParticipation(teamRoomId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAMROOM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("확정된 규칙 조회")
    class GetTeamRules {

        @Test
        @DisplayName("성공: 확정된 규칙 조회")
        void getTeamRules_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);
            TeamRule rule1 = mock(TeamRule.class);
            TeamRule rule2 = mock(TeamRule.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(teamRuleRepository.findByTeamRoomId(teamRoomId))
                    .willReturn(List.of(rule1, rule2));
            given(rule1.getId()).willReturn(1L);
            given(rule1.getContent()).willReturn("규칙1");
            given(rule2.getId()).willReturn(2L);
            given(rule2.getContent()).willReturn("규칙2");

            // when
            TeamRulesResponse response = ruleService.getTeamRules(teamRoomId, userId);

            // then
            assertThat(response.teamRules()).hasSize(2);
            assertThat(response.teamRules().getFirst().content()).isEqualTo("규칙1");
        }
    }

    @Nested
    @DisplayName("규칙 추가")
    class AddTeamRule {

        @Test
        @DisplayName("성공: 규칙 추가")
        void addTeamRule_Success() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            TeamRuleRequest request = new TeamRuleRequest("새로운 규칙");

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember leader = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(leader));
            given(leader.hasManagementAuthority()).willReturn(true);

            // when
            ruleService.addTeamRule(teamRoomId, request, userId);

            // then
            then(teamRuleRepository).should().save(any(TeamRule.class));
            then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("실패: 팀장 권한 없음")
        void addTeamRule_Fail_NotLeader() {
            // given
            Long teamRoomId = 1L;
            Long userId = 1L;
            TeamRuleRequest request = new TeamRuleRequest("새로운 규칙");

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember member = mock(TeamMember.class);

            given(teamRoomRepository.findById(teamRoomId)).willReturn(Optional.of(teamRoom));
            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(member));
            given(member.hasManagementAuthority()).willReturn(false);

            // when & then
            assertThatThrownBy(() -> ruleService.addTeamRule(teamRoomId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_TEAM_MANAGER);
        }
    }

    @Nested
    @DisplayName("규칙 수정")
    class UpdateTeamRule {

        @Test
        @DisplayName("성공: 규칙 수정")
        void updateTeamRule_Success() {
            // given
            Long teamRoomId = 1L;
            Long teamRuleId = 1L;
            Long userId = 1L;
            TeamRuleRequest request = new TeamRuleRequest("수정된 규칙");

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember leader = mock(TeamMember.class);
            TeamRule teamRule = mock(TeamRule.class);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(leader));
            given(leader.hasManagementAuthority()).willReturn(true);
            given(teamRuleRepository.findById(teamRuleId)).willReturn(Optional.of(teamRule));
            given(teamRule.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(teamRoomId);

            // when
            ruleService.updateTeamRule(teamRoomId, teamRuleId, request, userId);

            // then
            then(teamRule).should().updateContent("수정된 규칙");
            then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("실패: 규칙을 찾을 수 없음")
        void updateTeamRule_Fail_RuleNotFound() {
            // given
            Long teamRoomId = 1L;
            Long teamRuleId = 999L;
            Long userId = 1L;
            TeamRuleRequest request = new TeamRuleRequest("수정된 규칙");

            TeamMember leader = mock(TeamMember.class);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(leader));
            given(leader.hasManagementAuthority()).willReturn(true);
            given(teamRuleRepository.findById(teamRuleId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ruleService.updateTeamRule(teamRoomId, teamRuleId, request, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RULE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("규칙 삭제")
    class DeleteTeamRule {

        @Test
        @DisplayName("성공: 규칙 삭제")
        void deleteTeamRule_Success() {
            // given
            Long teamRoomId = 1L;
            Long teamRuleId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember leader = mock(TeamMember.class);
            TeamRule teamRule = mock(TeamRule.class);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(leader));
            given(leader.hasManagementAuthority()).willReturn(true);
            given(teamRuleRepository.findById(teamRuleId)).willReturn(Optional.of(teamRule));
            given(teamRule.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(teamRoomId);

            // when
            ruleService.deleteTeamRule(teamRoomId, teamRuleId, userId);

            // then
            then(teamRuleRepository).should().delete(teamRule);
            then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("실패: 다른 팀룸의 규칙 삭제 시도")
        void deleteTeamRule_Fail_WrongTeamRoom() {
            // given
            Long teamRoomId = 1L;
            Long teamRuleId = 1L;
            Long userId = 1L;

            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamMember leader = mock(TeamMember.class);
            TeamRule teamRule = mock(TeamRule.class);

            given(teamMemberRepository.findByTeamRoomIdAndUserId(teamRoomId, userId))
                    .willReturn(Optional.of(leader));
            given(leader.hasManagementAuthority()).willReturn(true);
            given(teamRuleRepository.findById(teamRuleId)).willReturn(Optional.of(teamRule));
            given(teamRule.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(999L); // 다른 팀룸

            // when & then
            assertThatThrownBy(() -> ruleService.deleteTeamRule(teamRoomId, teamRuleId, userId))
                    .isInstanceOf(YamoyoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RULE_NOT_FOUND);
        }
    }
}