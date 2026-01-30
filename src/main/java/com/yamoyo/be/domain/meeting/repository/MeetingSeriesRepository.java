package com.yamoyo.be.domain.meeting.repository;

import com.yamoyo.be.domain.meeting.entity.MeetingSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingSeriesRepository extends JpaRepository<MeetingSeries, Long> {
}
