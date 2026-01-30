package com.yamoyo.be.domain.rule.repository;

import com.yamoyo.be.domain.rule.entity.MemberRuleVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRuleVoteRepository extends JpaRepository<MemberRuleVote, Long> {

    // 팀원의 규칙 투표 조회 (중복 투표 방지용)
    Optional<MemberRuleVote> findByMemberIdAndRuleTemplateId(Long memberId, Long ruleTemplateId);

    // 팀룸의 모든 투표 조회
    List<MemberRuleVote> findByTeamRoomId(Long teamRoomId);

    // 팀룸에서 특정 규칙에 대한 투표 결과 집계
    @Query("SELECT COUNT(v) FROM MemberRuleVote v " +
            "WHERE v.teamRoom.id = :teamRoomId AND v.ruleTemplate.id = :ruleTemplateId AND v.isAgree = true")
    long countAgreeVotesByTeamRoomAndRule(@Param("teamRoomId") Long teamRoomId,
                                          @Param("ruleTemplateId") Long ruleTemplateId);

    // 팀룸에서 투표 완료한 팀원 수
    @Query("SELECT COUNT(DISTINCT v.member.id) FROM MemberRuleVote v WHERE v.teamRoom.id = :teamRoomId")
    long countDistinctMembersByTeamRoom(@Param("teamRoomId") Long teamRoomId);
}