package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TeamRoomRepository extends JpaRepository<TeamRoom,Long> {

    /**
     * 아카이빙 대상 팀룸 조회
     * - deadline이 기준일보다 이전인 팀룸
     */
    List<TeamRoom> findByLifecycleAndDeadlineBefore(Lifecycle lifecycle, LocalDateTime threshold);

    /**
     * 특정 라이프사이클이고 정확한 마감일인 팀룸 조회
     */
    List<TeamRoom> findByLifecycleAndDeadline(
            Lifecycle lifecycle,
            LocalDateTime deadline
    );
}
