package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeamRoomSetupRepository extends JpaRepository<TeamRoomSetup, Long> {

    /**
     * 팀룸 ID로 Setup 조회
     */
    @Query("""
        SELECT s
        FROM TeamRoomSetup s
        WHERE s.teamRoom.id = :teamRoomId
        """)
    Optional<TeamRoomSetup> findByTeamRoomId(@Param("teamRoomId") Long teamRoomId);

    /**
     * 마감 시각이 지난 Setup 조회 (스케줄러용)
     * - 협업툴, 규칙, 정기회의 중 미완료된 항목이 있는 경우만 조회
     */
    @Query("""
    SELECT s
    FROM TeamRoomSetup s
    WHERE s.deadline < :now
    AND (s.toolCompleted = false OR s.ruleCompleted = false OR s.meetingCompleted = false)
    """)
    List<TeamRoomSetup> findExpiredSetups(@Param("now") LocalDateTime now);

    /**
     * Setup 정보를 조회하면서 DB 레벨에서 행 잠금
     * 확정 처리 로직에서 동시성 문제를 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM TeamRoomSetup s
        WHERE s.teamRoom.id = :teamRoomId
        """)
    Optional<TeamRoomSetup> findByTeamRoomIdForUpdate(@Param("teamRoomId") Long teamRoomId);

}
