package com.yamoyo.be.domain.collabtool.entity;

import com.yamoyo.be.domain.rule.entity.TeamRule;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_tools")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamTool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_tool_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id")
    private TeamRoom teamRoom;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "tool_id", nullable = false)
    private Integer toolId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 정적 팩토리 메서드 - 투표 확정 후 생성
    public static TeamTool create(TeamRoom teamRoom, Integer categoryId, Integer toolId) {
        TeamTool teamTool = new TeamTool();
        teamTool.teamRoom = teamRoom;
        teamTool.categoryId = categoryId;
        teamTool.toolId = toolId;
        return teamTool;
    }
}
