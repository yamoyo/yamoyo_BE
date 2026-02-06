package com.yamoyo.be.domain.rule.entity;

import com.yamoyo.be.domain.teamroom.entity.TeamMember;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버 규칙 투표 엔티티
 * - 팀원이 규칙 템플릿에 대해 동의/비동의 투표
 */

@Entity
@Table(name = "member_rule_votes",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_member_rule_votes",
                    columnNames = {"member_id", "rule_id"}
            )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRuleVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_vote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private TeamMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleTemplate ruleTemplate;

    @Column(name = "is_agree", nullable = false)
    private boolean isAgree;  // true: 동의, false: 비동의

    // 정적 팩토리 메서드
    public static MemberRuleVote create(
            TeamMember member,
            TeamRoom teamRoom,
            RuleTemplate ruleTemplate,
            boolean isAgree
    ) {
        MemberRuleVote vote = new MemberRuleVote();
        vote.member = member;
        vote.teamRoom = teamRoom;
        vote.ruleTemplate = ruleTemplate;
        vote.isAgree = isAgree;
        return vote;
    }

    /** 투표 수정 */
    public void updateAgreement(boolean isAgree) {
        this.isAgree = isAgree;
    }
}
