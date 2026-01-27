package com.yamoyo.be.domain.rule.entity;

import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 팀 규칙 엔티티
 * - 투표 확정 후 팀룸에 적용된 규칙
 */
@Entity
@Table(name = "team_rules")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_rule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoom;

    @Column(name = "content", length = 255)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 정적 팩토리 메서드 - 투표 확정 후 생성
    public static TeamRule create(TeamRoom teamRoom, String content) {
        TeamRule rule = new TeamRule();
        rule.teamRoom = teamRoom;
        rule.content = content;
        return rule;
    }

    // 팀장이 규칙 수정
    public void updateContent(String content) {
        this.content = content;
    }
}