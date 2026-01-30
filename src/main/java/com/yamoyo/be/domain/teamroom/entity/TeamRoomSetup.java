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
    @Column(name = "tool_completed", nullable = false, columnDefinition = "TINYINT")
    private Boolean toolCompleted;

    /**
     * 규칙 확정 완료 여부
     * 0: 미완료, 1: 완료
     */
    @Column(name = "rule_completed", nullable = false, columnDefinition = "TINYINT")
    private Boolean ruleCompleted;

    /**
     * 정기회의 확정 완료 여부
     * 0: 미완료, 1: 완료
     */
    @Column(name = "meeting_completed", nullable = false, columnDefinition = "TINYINT")
    private Boolean meetingCompleted;

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
        this.deadline = this.createdAt.plusHours(6);
    }

    /**
     * Setup 생성
     * TODO : 팀장 정하기 완료 시 호출하는 로직
     */
    public static TeamRoomSetup create(TeamRoom teamRoom) {
        TeamRoomSetup setup = new TeamRoomSetup();
        setup.teamRoom = teamRoom;
        setup.toolCompleted = false;
        setup.ruleCompleted = false;
        setup.meetingCompleted = false;
        return setup;
    }

    /**
     * 협업툴 확정 완료 처리
     */
    public void completeToolSetup() {
        this.toolCompleted = true;
    }

    /**
     * 규칙 확정 완료 처리
     */
    public void completeRuleSetup() {
        this.ruleCompleted = true;
    }

    /**
     * 정기회의 확정 완료 처리
     */
    public void completeMeetingSetup() {
        this.meetingCompleted = true;
    }

    /**
     * 협업툴 확정 여부
     */
    public boolean isToolCompleted() {
        return this.toolCompleted;
    }

    /**
     * 규칙 확정 여부
     */
    public boolean isRuleCompleted() {
        return this.ruleCompleted;
    }

    /**
     * 정기회의 확정 여부
     */
    public boolean isMeetingCompleted() {
        return this.meetingCompleted;
    }

    /**
     * 모든 설정 완료 여부
     */
    public boolean isAllCompleted() {
        return toolCompleted && ruleCompleted && meetingCompleted;
    }
}
