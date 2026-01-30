package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TeamRoomRepository extends JpaRepository<TeamRoom,Long> {

    /**
     * 아카이빙 대상 팀룸 조회
     * - lifecycle = ACTIVE
     * - deadline + 7일이 지난 팀룸
     */
    List<TeamRoom> findByLifecycleAndDeadlineBefore(Lifecycle lifecycle, LocalDateTime deadline);
}
