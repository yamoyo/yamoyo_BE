package com.yamoyo.be.domain.collabtool.repository;

import com.yamoyo.be.domain.collabtool.entity.TeamTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamToolRepository extends JpaRepository<TeamTool,Long> {

    /**
     * 팀룸의 확정된 협업툴 조회
     */
    @Query("""
        SELECT tt
        FROM TeamTool tt
        WHERE tt.teamRoom.id = :teamRoomId
        """)
    List<TeamTool> findByTeamRoomId(@Param("teamRoomId") Long teamRoomId);

    /**
     * 특정 협업툴이 해당 팀룸에 속하는지 확인
     */
    @Query("""
        SELECT COUNT(tt) > 0
        FROM TeamTool tt
        WHERE tt.id = :teamToolId AND tt.teamRoom.id = :teamRoomId
        """)
    boolean existsByIdAndTeamRoomId(
            @Param("teamToolId") Long teamToolId,
            @Param("teamRoomId") Long teamRoomId
    );
}
