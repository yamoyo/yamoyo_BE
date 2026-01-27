package com.yamoyo.be.domain.rule.repository;

import com.yamoyo.be.domain.rule.entity.TeamRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRuleRepository extends JpaRepository<TeamRule, Long> {

    /**
     * 팀룸의 확정 규칙 목록 조회
     */
    List<TeamRule> findByTeamRoomId(Long teamRoomId);

}
