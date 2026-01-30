package com.yamoyo.be.domain.meeting.repository;

import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimepickParticipantRepository extends JpaRepository<TimepickParticipant, Long> {
    Optional<TimepickParticipant> findByTimepickIdAndUserId(Long timepickId, Long userId);

    @Query("""
            SELECT tp FROM TimepickParticipant tp
            JOIN FETCH tp.user
            WHERE tp.timepick.id = :timepickId
            """)
    List<TimepickParticipant> findByTimepickIdWithUser(@Param("timepickId") Long timepickId);
}
