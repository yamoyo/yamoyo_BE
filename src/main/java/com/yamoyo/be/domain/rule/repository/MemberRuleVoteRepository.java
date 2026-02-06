package com.yamoyo.be.domain.rule.repository;

import com.yamoyo.be.domain.rule.entity.MemberRuleVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRuleVoteRepository extends JpaRepository<MemberRuleVote, Long> {

    /** 팀원의 규칙 투표 조회 (중복 투표 방지용) */
    Optional<MemberRuleVote> findByTeamRoomIdAndMemberIdAndRuleTemplateId(
            Long teamRoomId, Long memberId, Long ruleTemplateId
    );

    /** 팀룸의 모든 투표 조회 */
    List<MemberRuleVote> findByTeamRoomId(Long teamRoomId);

    /**
     * 규칙별 동의 득표 수 집계 (DB 레벨)
     */
    @Query("""
        SELECT v.ruleTemplate.id, COUNT(v)
        FROM MemberRuleVote v
        WHERE v.teamRoom.id = :teamRoomId
          AND v.isAgree = true
          AND v.member.id IN :memberIds
        GROUP BY v.ruleTemplate.id
    """)
    List<Object[]> countAgreeVotesByRuleForMembers(@Param("teamRoomId") Long teamRoomId,
                                                   @Param("memberIds") List<Long> memberIds);


    /** 팀룸에서 투표 완료한 팀원 수 */
    @Query("""
        SELECT v.member.id
        FROM MemberRuleVote v
        WHERE v.teamRoom.id = :teamRoomId
        GROUP BY v.member.id
        HAVING COUNT(DISTINCT v.ruleTemplate.id) = :totalRules
    """)
    List<Long> findMemberIdsWhoCompletedAllRules(@Param("teamRoomId") Long teamRoomId,
                                                 @Param("totalRules") long totalRules);

}