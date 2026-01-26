package com.yamoyo.be.domain.teamroom.repository;

import com.yamoyo.be.domain.teamroom.entity.BannedTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannedTeamMemberRepository extends JpaRepository<BannedTeamMember, Long> {

    /**
     * 특정 팀룸에서 특정 유저가 밴 되었는지 확인
     */
    boolean existsByTeamRoomIdAndUserId(Long teamRoomId, Long userId);
}