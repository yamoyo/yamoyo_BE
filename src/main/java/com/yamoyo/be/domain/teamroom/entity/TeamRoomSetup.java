package com.yamoyo.be.domain.teamroom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_room_setups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamRoomSetup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setup_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false, unique = true)
    private TeamRoom teamRoom;

    /**
     * 협업툴 확정 완료 여부
     * 0: 미완료, 1: 완료
     */
    @Column(name = "tool_completed", nullable = false)
    private Integer toolCompleted;

    /**
     * 규칙 확정 완료 여부
     * 0: 미완료, 1: 완료
     */
    @Column(name = "rule_completed", nullable = false)
    private Integer ruleCompleted;

    /**
     * 정기회의 확정 완료 여부
     * 0: 미완료, 1: 완료
     */
    @Column(name = "meeting_completed", nullable = false)
    private Integer meetingCompleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 자동 확정 마감 시각 (생성 시각 + 6시간)
     */
    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Setup 생성 (팀장 정하기 완료 시 호출)
     */
    public static TeamRoomSetup create(TeamRoom teamRoom) {
        TeamRoomSetup setup = new TeamRoomSetup();
        setup.teamRoom = teamRoom;
        setup.toolCompleted = 0;
        setup.ruleCompleted = 0;
        setup.meetingCompleted = 0;
        setup.deadline = LocalDateTime.now().plusHours(6);
        return setup;
    }

    /**
     * 협업툴 확정 완료 처리
     */
    public void completeToolSetup() {
        this.toolCompleted = 1;
    }

    /**
     * 규칙 확정 완료 처리
     */
    public void completeRuleSetup() {
        this.ruleCompleted = 1;
    }

    /**
     * 정기회의 확정 완료 처리
     */
    public void completeMeetingSetup() {
        this.meetingCompleted = 1;
    }

    /**
     * 협업툴 확정 여부
     */
    public boolean isToolCompleted() {
        return this.toolCompleted == 1;
    }

    /**
     * 규칙 확정 여부
     */
    public boolean isRuleCompleted() {
        return this.ruleCompleted == 1;
    }

    /**
     * 정기회의 확정 여부
     */
    public boolean isMeetingCompleted() {
        return this.meetingCompleted == 1;
    }

    /**
     * 모든 설정 완료 여부
     */
    public boolean isAllCompleted() {
        return toolCompleted == 1 && ruleCompleted == 1 && meetingCompleted == 1;
    }

    /**
     * 마감 시각 지났는지 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(deadline);
    }
}
