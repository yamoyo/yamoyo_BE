package com.yamoyo.be.domain.meeting.repository;

import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimepickParticipantRepository extends JpaRepository<TimepickParticipant, Long> {
    Optional<TimepickParticipant> findByTimepickIdAndUserId(Long timepickId, Long userId);
}
