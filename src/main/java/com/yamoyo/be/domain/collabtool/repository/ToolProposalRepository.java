package com.yamoyo.be.domain.collabtool.repository;

import com.yamoyo.be.domain.collabtool.entity.ToolProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ToolProposalRepository extends JpaRepository<ToolProposal, Long> {

    /**
     * 제안 중인(PENDING) 협업툴 조회
     */
    @Query("""
        SELECT tp
        FROM ToolProposal tp
        WHERE tp.teamRoom.id = :teamRoomId
        AND tp.isApproval = 0
        """)
    List<ToolProposal> findPendingProposalsByTeamRoomId(@Param("teamRoomId") Long teamRoomId);

    /**
     * 제안이 해당 팀룸에 속하는지 확인
     */
    @Query("""
        SELECT COUNT(tp) > 0
        FROM ToolProposal tp
        WHERE tp.id = :proposalId AND tp.teamRoom.id = :teamRoomId
        """)
    boolean existsByIdAndTeamRoomId(
            @Param("proposalId") Long proposalId,
            @Param("teamRoomId") Long teamRoomId
    );
}