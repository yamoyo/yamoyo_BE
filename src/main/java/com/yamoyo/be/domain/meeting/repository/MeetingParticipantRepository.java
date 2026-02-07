package com.yamoyo.be.domain.meeting.repository;

import com.yamoyo.be.domain.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    @Query("""
            SELECT mp.meeting.id, COUNT(mp)
            FROM MeetingParticipant mp
            WHERE mp.meeting.id IN :meetingIds
            GROUP BY mp.meeting.id
            """)
    List<Object[]> countByMeetingIds(@Param("meetingIds") List<Long> meetingIds);

    @Query("""
            SELECT mp FROM MeetingParticipant mp
            JOIN FETCH mp.user
            WHERE mp.meeting.id = :meetingId
            """)
    List<MeetingParticipant> findByMeetingIdWithUser(@Param("meetingId") Long meetingId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    @Modifying
    @Query("DELETE FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId")
    void deleteByMeetingId(@Param("meetingId") Long meetingId);

    @Modifying
    @Query("DELETE FROM MeetingParticipant mp WHERE mp.meeting.id IN :meetingIds")
    void deleteByMeetingIdIn(@Param("meetingIds") List<Long> meetingIds);
}
