package com.yamoyo.be.domain.meeting.repository;

import com.yamoyo.be.domain.meeting.entity.Timepick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimepickRepository extends JpaRepository<Timepick, Long> {
    Optional<Timepick> findByTeamRoomId(Long teamRoomId);
}
