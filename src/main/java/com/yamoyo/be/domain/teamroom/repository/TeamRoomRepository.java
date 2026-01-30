package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TeamRoomRepository extends JpaRepository<TeamRoom,Long> {

    /**
     * 아카이빙 대상 팀룸 조회
     * - deadline + 7일 23:59:59 이전인 팀룸
     */
    @Query("SELECT tr FROM TeamRoom tr " +
            "WHERE tr.lifecycle = :lifecycle " +
            "AND DATE(tr.deadline) < DATE(:currentDate) - 7")
    List<TeamRoom> findByLifecycleAndDeadlineBefore(
            @Param("lifecycle") Lifecycle lifecycle,
            @Param("currentDate") LocalDate currentDate
    );

}
