package com.yamoyo.be.domain.teamroom.scheduler;

import com.yamoyo.be.domain.notification.entity.NotificationType;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import com.yamoyo.be.event.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 아카이빙 처리를 위한 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamRoomScheduler {

    private final TeamRoomRepository teamRoomRepository;
    private final ApplicationEventPublisher eventPublisher;


    private static final int ARCHIVE_DELAY_DAYS = 7;

    /**
     * 매일 자정 실행 - 아카이빙 처리
     * deadline + 7일이 지난 ACTIVE 팀룸을 ARCHIVED로 변경
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void archiveExpiredTeamRooms() {
        log.info("=== 팀룸 아카이빙 스케줄러 시작 ===");

        // 오늘 - 7일 계산 (00:00:00 기준)
        LocalDateTime archiveThreshold = LocalDate.now()
                .minusDays(7)
                .atStartOfDay();

        // 아카이빙 대상 조회
        List<TeamRoom> teamRoomsToArchive = teamRoomRepository.findByLifecycleAndDeadlineBefore(
                Lifecycle.ACTIVE,
                archiveThreshold
        );

        if (teamRoomsToArchive.isEmpty()) {
            log.info("아카이빙 대상 팀룸이 없습니다.");
            return;
        }

        // 아카이빙 처리
        int archivedCount = 0;
        for (TeamRoom teamRoom : teamRoomsToArchive) {
            try {
                teamRoom.archive();

                eventPublisher.publishEvent(NotificationEvent.ofSingle(
                        teamRoom.getId(),
                        teamRoom.getId(),
                        NotificationType.TEAM_ARCHIVED
                ));

                log.info("팀룸 아카이빙 완료 - ID: {}, 제목: {}", teamRoom.getId(), teamRoom.getTitle());

                archivedCount++;

            } catch (Exception e) {
                log.error("팀룸 아카이빙 실패 - ID: {}, 에러: {}", teamRoom.getId(), e.getMessage());
            }
        }

        log.info("=== 팀룸 아카이빙 스케줄러 종료 - 총 {}개 처리 ===", archivedCount);
    }

    /**
     * 매일 오전 9시에 마감 D-1 팀룸에 알림 발송
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendDeadlineReminder() {
        log.info("=== 마감 D-1 알림 스케줄러 시작 ===");

        try {
            // 내일 23:59:59로 마감되는 팀룸 조회
            LocalDateTime tomorrowDeadline = LocalDate.now()
                    .plusDays(1)
                    .atTime(23, 59, 59);

            List<TeamRoom> deadlineSoonRooms = teamRoomRepository
                    .findByLifecycleAndDeadline(
                            Lifecycle.ACTIVE,
                            tomorrowDeadline
                    );

            if (deadlineSoonRooms.isEmpty()) {
                log.info("마감 D-1 팀룸 없음");
                return;
            }

            // 각 팀룸에 알림 발송
            int sentCount = 0;
            for (TeamRoom teamRoom : deadlineSoonRooms) {
                try {
                    eventPublisher.publishEvent(NotificationEvent.ofSingle(
                            teamRoom.getId(),
                            teamRoom.getId(),
                            NotificationType.TEAM_DEADLINE_REMIND
                    ));
                    sentCount++;
                    log.info("마감 D-1 알림 발송 완료 - ID: {}, 제목: {}",
                            teamRoom.getId(), teamRoom.getTitle());
                } catch (Exception e) {
                    log.error("마감 D-1 알림 발송 실패 - ID: {}, 에러: {}",
                            teamRoom.getId(), e.getMessage());
                }
            }

            log.info("=== 마감 D-1 알림 스케줄러 종료 - 총 {}개 발송 ===", sentCount);

        } catch (Exception e) {
            log.error("마감 D-1 알림 스케줄러 실행 중 오류 발생", e);
        }
    }
}