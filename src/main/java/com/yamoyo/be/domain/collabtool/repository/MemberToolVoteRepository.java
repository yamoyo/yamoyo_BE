package com.yamoyo.be.domain.collabtool.repository;

import com.yamoyo.be.domain.collabtool.entity.MemberToolVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberToolVoteRepository extends JpaRepository<MemberToolVote,Long> {

    /**
     * 카테고리별 득표 현황 조회
     */
    @Query("""
        SELECT tv.toolId, COUNT(tv)
        FROM MemberToolVote tv
        WHERE tv.teamRoomId = :teamRoomId
        AND tv.categoryId = :categoryId
        GROUP BY tv.toolId
        """)
    List<Object[]> countVotesByCategory(
            @Param("teamRoomId") Long teamRoomId,
            @Param("categoryId") Integer categoryId
    );

    /**
     * 투표 참여 현황 조회 (일괄 제출 완료한 멤버)
     * - 일괄 제출이므로 한번이라도 투표한 멤버 = 모든 카테고리 완료
     */
    @Query("""
        SELECT DISTINCT tv.memberId
        FROM MemberToolVote tv
        WHERE tv.teamRoomId = :teamRoomId
        """)
    List<Long> findVotedMemberIds(@Param("teamRoomId") Long teamRoomId);

    /**
     * 특정 멤버가 이미 투표했는지 확인 (중복 투표 방지)
     */
    boolean existsByMemberId(Long memberId);
}
