package com.yamoyo.be.domain.teamroom.service;

import com.yamoyo.be.domain.collabtool.service.ToolService;
import com.yamoyo.be.domain.rule.service.RuleService;
import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomSetupRepository;
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
                // 협업툴 미확정 시 자동 확정
                if (!setup.isToolCompleted()) {
                    log.info("협업툴 자동 확정 시작 - teamRoomId: {}", teamRoomId);
                    toolService.confirmTools(teamRoomId);
                    setup.completeToolSetup();
                    log.info("협업툴 자동 확정 완료 - teamRoomId: {}", teamRoomId);
                }

                // 규칙 미확정 시 자동 확정
                if (!setup.isRuleCompleted()) {
                    log.info("규칙 자동 확정 시작 - teamRoomId: {}", teamRoomId);
                    ruleService.confirmRules(teamRoomId);
                    setup.completeRuleSetup();
                    log.info("규칙 자동 확정 완료 - teamRoomId: {}", teamRoomId);
                }

                // 정기회의는 추후 추가
                // if (!setup.isMeetingCompleted()) {
                //     meetingService.confirmMeeting(teamRoomId);
                //     setup.completeMeetingSetup();
                // }

            } catch (Exception e) {
                log.error("Setup 자동 확정 실패 - teamRoomId: {}, error: {}", teamRoomId, e.getMessage(), e);
            }
        }

        log.info("만료된 Setup 처리 완료");
    }
}
