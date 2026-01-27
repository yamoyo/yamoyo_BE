package com.yamoyo.be.domain.meeting.repository;

import com.yamoyo.be.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @Query("""
            SELECT m FROM Meeting m
            JOIN FETCH m.meetingSeries ms
            JOIN FETCH ms.teamRoom
            WHERE m.id = :meetingId
            """)
    Optional<Meeting> findByIdWithSeriesAndTeamRoom(@Param("meetingId") Long meetingId);

    @Query("""
            SELECT m FROM Meeting m
            JOIN FETCH m.meetingSeries ms
            WHERE ms.teamRoom.id = :teamRoomId
              AND m.startTime >= :startDateTime
              AND m.startTime < :endDateTime
            ORDER BY m.startTime ASC
            """)
    List<Meeting> findByTeamRoomIdAndStartTimeBetween(
            @Param("teamRoomId") Long teamRoomId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            SELECT m FROM Meeting m
            JOIN FETCH m.meetingSeries ms
            WHERE ms.id = :meetingSeriesId AND m.startTime >= :startTime
            ORDER BY m.startTime ASC
            """)
    List<Meeting> findByMeetingSeriesIdAndStartTimeGreaterThanEqual(
            @Param("meetingSeriesId") Long meetingSeriesId,
            @Param("startTime") LocalDateTime startTime
    );
}
