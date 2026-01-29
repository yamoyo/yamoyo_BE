package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.TeamRoomSetup;
import org.springframework.data.jpa.repository.JpaRepository;
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
    AND (s.toolCompleted = 0 OR s.ruleCompleted = 0 OR s.meetingCompleted = 0)
    """)
    List<TeamRoomSetup> findExpiredSetups(@Param("now") LocalDateTime now);
}
