package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.collabtool.service.ToolService;
import com.yamoyo.be.domain.rule.service.RuleService;
import com.yamoyo.be.domain.teamroom.scheduler.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
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
            } catch (YamoyoException e) {
                log.error("협업툴 확정 실패 - teamRoomId: {}, errorCode: {}",
                        teamRoomId, e.getErrorCode(), e);
            }

            try {
                // 규칙 미확정 시 처리
                if (!setup.isRuleCompleted()) {
                    ruleService.confirmRules(teamRoomId);
                    setup.completeRuleSetup();
                }
            } catch (YamoyoException e) {
                log.error("규칙 확정 실패 - teamRoomId: {}, errorCode: {}",
                        teamRoomId, e.getErrorCode(), e);
            }

            // ===== 정기회의는 추후 추가 =====
            // TODO : 정기 회의 자동 확정 처리 로직
            // try {
            //     if (!setup.isMeetingCompleted()) {
            //         meetingService.confirmMeeting(teamRoomId);
            //         setup.completeMeetingSetup();
            //     }
            // } catch (YamoyoException e) {
            //     log.error("정기회의 확정 실패 - teamRoomId: {}, errorCode: {}",
            //         teamRoomId, e.getErrorCode(), e);
            // }
                    }

        log.info("만료된 Setup 처리 완료");
    }
}
