package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRoomRepository extends JpaRepository<TeamRoom,Long> {
    // JPA 기본 메서드만 사용
}
