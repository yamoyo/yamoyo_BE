package com.yamoyo.be.domain.teamroom.scheduler;

import com.yamoyo.be.domain.collabtool.service.ToolService;
import com.yamoyo.be.domain.meeting.entity.Timepick;
import com.yamoyo.be.domain.meeting.repository.TimepickRepository;
import com.yamoyo.be.domain.meeting.service.TimepickService;
import com.yamoyo.be.domain.rule.service.RuleService;
import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetupScheduler {

    private final TeamRoomSetupRepository setupRepository;
    private final ToolService toolService;
    private final RuleService ruleService;
    private final TimepickService timepickService;
    private final TimepickRepository timepickRepository;

    /**
     * 1분마다 만료된 Setup 확인 및 자동 확정 처리
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processExpiredSetups() {
        log.info("만료된 Setup 확인 시작");

        LocalDateTime now = LocalDateTime.now();
        List<TeamRoomSetup> expiredSetups = setupRepository.findExpiredSetups(now);

        if (expiredSetups.isEmpty()) {
            log.debug("만료된 Setup 없음");
            return;
        }

        log.info("만료된 Setup 발견: {}개", expiredSetups.size());

        for (TeamRoomSetup setup : expiredSetups) {
            Long teamRoomId = setup.getTeamRoom().getId();

            try {
                // 협업툴 미확정 시 처리
                if (!setup.isToolCompleted()) {
                    toolService.confirmTools(teamRoomId);
                    setup.completeToolSetup();
                }
            } catch (Exception e) {
                log.error("협업툴 확정 실패 - teamRoomId: {}",
                        teamRoomId, e);
            }

            try {
                // 규칙 미확정 시 처리
                if (!setup.isRuleCompleted()) {
                    ruleService.confirmRules(teamRoomId);
                    setup.completeRuleSetup();
                }
            } catch (Exception e) {
                log.error("규칙 확정 실패 - teamRoomId: {}",
                        teamRoomId, e);
            }

            // 정기회의 미확정 시 처리
            try {
                if (!setup.isMeetingCompleted()) {
                    Timepick timepick = timepickRepository.findByTeamRoomId(teamRoomId)
                            .orElseThrow(() -> new YamoyoException(ErrorCode.TIMEPICK_NOT_FOUND));

                    if (!timepick.isFinalized()) {
                        timepickService.finalizeTimepick(timepick.getId());
                    }
                    setup.completeMeetingSetup();
                }
            } catch (Exception e) {
                log.error("정기회의 확정 실패 - teamRoomId: {}", teamRoomId, e);
            }

            // 모든 설정이 완료되면 TeamRoom의 workflow를 COMPLETED로 변경
            if (setup.isAllCompleted()) {
                try {
                    setup.getTeamRoom().completeSetup();
                    log.info("팀룸 Setup 완료 처리 - teamRoomId: {}", teamRoomId);
                } catch (Exception e) {
                    log.error("팀룸 Setup 완료 처리 실패 - teamRoomId: {}", teamRoomId, e);
                }
            }
        }

        log.info("만료된 Setup 처리 완료");
    }
}
