package com.yamoyo.be.domain.teamroom.scheduler;

import com.yamoyo.be.domain.collabtool.service.ToolService;
import com.yamoyo.be.domain.rule.service.RuleService;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
import com.yamoyo.be.domain.teamroom.service.SetupScheduler;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SetupScheduler 단위 테스트")
class SetupSchedulerTest {

    @InjectMocks
    private SetupScheduler setupScheduler;

    @Mock
    private TeamRoomSetupRepository setupRepository;

    @Mock
    private ToolService toolService;

    @Mock
    private RuleService ruleService;

    @Nested
    @DisplayName("만료된 Setup 처리")
    class ProcessExpiredSetups {

        @Test
        @DisplayName("성공: 만료된 Setup이 없는 경우")
        void processExpiredSetups_Success_NoExpiredSetups() {
            // given
            given(setupRepository.findExpiredSetups(any())).willReturn(Collections.emptyList());

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should(never()).confirmTools(anyLong());
            then(ruleService).should(never()).confirmRules(anyLong());
        }

        @Test
        @DisplayName("성공: 협업툴과 규칙 모두 미확정인 경우")
        void processExpiredSetups_Success_BothNotCompleted() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup));
            given(setup.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(1L);
            given(setup.isToolCompleted()).willReturn(false);
            given(setup.isRuleCompleted()).willReturn(false);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should().confirmTools(1L);
            then(setup).should().completeToolSetup();
            then(ruleService).should().confirmRules(1L);
            then(setup).should().completeRuleSetup();
        }

        @Test
        @DisplayName("성공: 협업툴만 미확정인 경우")
        void processExpiredSetups_Success_OnlyToolNotCompleted() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup));
            given(setup.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(1L);
            given(setup.isToolCompleted()).willReturn(false);
            given(setup.isRuleCompleted()).willReturn(true);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should().confirmTools(1L);
            then(setup).should().completeToolSetup();
            then(ruleService).should(never()).confirmRules(anyLong());
            then(setup).should(never()).completeRuleSetup();
        }

        @Test
        @DisplayName("성공: 규칙만 미확정인 경우")
        void processExpiredSetups_Success_OnlyRuleNotCompleted() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup));
            given(setup.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(1L);
            given(setup.isToolCompleted()).willReturn(true);
            given(setup.isRuleCompleted()).willReturn(false);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should(never()).confirmTools(anyLong());
            then(setup).should(never()).completeToolSetup();
            then(ruleService).should().confirmRules(1L);
            then(setup).should().completeRuleSetup();
        }

        @Test
        @DisplayName("성공: 모두 확정된 경우")
        void processExpiredSetups_Success_AllCompleted() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup));
            given(setup.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(1L);
            given(setup.isToolCompleted()).willReturn(true);
            given(setup.isRuleCompleted()).willReturn(true);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should(never()).confirmTools(anyLong());
            then(setup).should(never()).completeToolSetup();
            then(ruleService).should(never()).confirmRules(anyLong());
            then(setup).should(never()).completeRuleSetup();
        }

        @Test
        @DisplayName("성공: 협업툴 확정 실패 시 규칙은 계속 진행")
        void processExpiredSetups_Success_ToolConfirmFails() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup));
            given(setup.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(1L);
            given(setup.isToolCompleted()).willReturn(false);
            given(setup.isRuleCompleted()).willReturn(false);
            willThrow(new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND))
                    .given(toolService).confirmTools(1L);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should().confirmTools(1L);
            then(setup).should(never()).completeToolSetup();
            then(ruleService).should().confirmRules(1L);
            then(setup).should().completeRuleSetup();
        }

        @Test
        @DisplayName("성공: 규칙 확정 실패 시 예외 처리")
        void processExpiredSetups_Success_RuleConfirmFails() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup));
            given(setup.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(1L);
            given(setup.isToolCompleted()).willReturn(false);
            given(setup.isRuleCompleted()).willReturn(false);
            willThrow(new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND))
                    .given(ruleService).confirmRules(1L);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should().confirmTools(1L);
            then(setup).should().completeToolSetup();
            then(ruleService).should().confirmRules(1L);
            then(setup).should(never()).completeRuleSetup();
        }

        @Test
        @DisplayName("성공: 협업툴과 규칙 모두 확정 실패")
        void processExpiredSetups_Success_BothConfirmFails() {
            // given
            TeamRoom teamRoom = mock(TeamRoom.class);
            TeamRoomSetup setup = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup));
            given(setup.getTeamRoom()).willReturn(teamRoom);
            given(teamRoom.getId()).willReturn(1L);
            given(setup.isToolCompleted()).willReturn(false);
            given(setup.isRuleCompleted()).willReturn(false);
            willThrow(new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND))
                    .given(toolService).confirmTools(1L);
            willThrow(new YamoyoException(ErrorCode.TEAMROOM_NOT_FOUND))
                    .given(ruleService).confirmRules(1L);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should().confirmTools(1L);
            then(setup).should(never()).completeToolSetup();
            then(ruleService).should().confirmRules(1L);
            then(setup).should(never()).completeRuleSetup();
        }

        @Test
        @DisplayName("성공: 여러 개의 만료된 Setup 처리")
        void processExpiredSetups_Success_MultipleExpiredSetups() {
            // given
            TeamRoom teamRoom1 = mock(TeamRoom.class);
            TeamRoomSetup setup1 = mock(TeamRoomSetup.class);

            TeamRoom teamRoom2 = mock(TeamRoom.class);
            TeamRoomSetup setup2 = mock(TeamRoomSetup.class);

            given(setupRepository.findExpiredSetups(any())).willReturn(List.of(setup1, setup2));

            // Setup 1
            given(setup1.getTeamRoom()).willReturn(teamRoom1);
            given(teamRoom1.getId()).willReturn(1L);
            given(setup1.isToolCompleted()).willReturn(false);
            given(setup1.isRuleCompleted()).willReturn(false);

            // Setup 2
            given(setup2.getTeamRoom()).willReturn(teamRoom2);
            given(teamRoom2.getId()).willReturn(2L);
            given(setup2.isToolCompleted()).willReturn(false);
            given(setup2.isRuleCompleted()).willReturn(true);

            // when
            setupScheduler.processExpiredSetups();

            // then
            then(toolService).should().confirmTools(1L);
            then(setup1).should().completeToolSetup();
            then(ruleService).should().confirmRules(1L);
            then(setup1).should().completeRuleSetup();

            then(toolService).should().confirmTools(2L);
            then(setup2).should().completeToolSetup();
            then(ruleService).should(never()).confirmRules(2L);
            then(setup2).should(never()).completeRuleSetup();
        }
    }
}
