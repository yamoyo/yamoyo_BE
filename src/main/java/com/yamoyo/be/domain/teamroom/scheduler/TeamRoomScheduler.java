package com.yamoyo.be.domain.teamroom.scheduler;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.repository.TeamRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 아카이빙 처리를 위한 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamRoomScheduler {

    private final TeamRoomRepository teamRoomRepository;

    private static final int ARCHIVE_DELAY_DAYS = 7;

    /**
     * 매일 자정 실행 - 아카이빙 처리
     * deadline + 7일이 지난 ACTIVE 팀룸을 ARCHIVED로 변경
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void archiveExpiredTeamRooms() {
        log.info("=== 팀룸 아카이빙 스케줄러 시작 ===");

        // 오늘 날짜 기준으로 조회
        LocalDate today = LocalDate.now();

        // 아카이빙 대상 조회 (deadline + 7일이 지난 팀룸)
        List<TeamRoom> teamRoomsToArchive = teamRoomRepository.findByLifecycleAndDeadlineBefore(
                Lifecycle.ACTIVE,
                today
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
                archivedCount++;
                log.info("팀룸 아카이빙 완료 - ID: {}, 제목: {}", teamRoom.getId(), teamRoom.getTitle());
            } catch (Exception e) {
                log.error("팀룸 아카이빙 실패 - ID: {}, 에러: {}", teamRoom.getId(), e.getMessage());
            }
        }

        log.info("=== 팀룸 아카이빙 스케줄러 종료 - 총 {}개 처리 ===", archivedCount);
    }
}