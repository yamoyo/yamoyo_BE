package com.yamoyo.be.domain.teamroom.entity;

import com.yamoyo.be.domain.teamroom.entity.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.entity.enums.Workflow;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_room_id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 20)
    private String title;

    @Column(name = "description", length = 50)
    private String description;

    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle")
    private Lifecycle lifecycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow")
    private Workflow workflow;

    @Column(name = "banner_image_id")
    private Long bannerImageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 기본 생성 빌더
    @Builder
    public TeamRoom(String title, String description, LocalDateTime deadline, Long bannerImageId) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.bannerImageId = bannerImageId;
        this.lifecycle = Lifecycle.ACTIVE;
        this.workflow = Workflow.PENDING;
    }

    // ===== 비즈니스 로직 ======
    /**
     * 팀룸 삭제
     */
    public void delete(){
        if(this.lifecycle == Lifecycle.DELETED){
            throw new IllegalStateException("이미 삭제된 팀룸입니다.");
        }
        // 아카이빙 된 팀룸은 삭제 불가
        if (this.lifecycle == Lifecycle.ARCHIVED) {
            throw new IllegalStateException("아카이빙된 팀룸은 삭제할 수 없습니다");
        }

        this.lifecycle = Lifecycle.DELETED;
    }

    /**
     * 팀룸 아카이빙 (마감일 +7일 후)
     */
    public void archive() {
        if (this.lifecycle != Lifecycle.ACTIVE) {
            throw new IllegalStateException("진행 중인 팀룸에서만 아카이빙 가능합니다");
        }
        this.lifecycle = Lifecycle.ARCHIVED;
    }

    /**
     * 팀룸 정보 수정 (제목, 설명)
     */
    public void update(String title, String description, LocalDateTime deadline, Long bannerImageId) {
        // 아카이빙된 팀룸은 수정 불가
        if (this.lifecycle == Lifecycle.ARCHIVED) {
            throw new IllegalStateException("아카이빙된 팀룸은 수정할 수 없습니다");
        }

        // 제목 수정
        if (title != null && !title.isBlank()) {
            this.title = title;
        }

        // 설명 수정
        if (description != null) {
            this.description = description;
        }

        // 마감일 수정
        if (deadline != null) {
            if (deadline.toLocalDate().isBefore(LocalDateTime.now().plusDays(1).toLocalDate())) {
                throw new IllegalArgumentException("마감일은 내일 이후로 설정해야 합니다.");
            }
            this.deadline = deadline;
        }

        // 배너 수정
        if (bannerImageId != null) {
            this.bannerImageId = bannerImageId;
        }
    }

    /**
     * 팀장 정하기 시작
     */
    public void startLeaderSelection() {
        if (this.workflow != Workflow.PENDING) {
            throw new IllegalStateException("대기 상태에서만 팀장 정하기를 시작할 수 있습니다");
        }
        this.workflow = Workflow.LEADER_SELECTION;
    }

    /**
     * 팀장 선출 완료 → SETUP 단계로 이동
     */
    public void completeLeaderSelection() {
        if (this.workflow != Workflow.LEADER_SELECTION) {
            throw new IllegalStateException("팀장 정하기 진행 중이 아닙니다");
        }
        this.workflow = Workflow.SETUP;
    }

    /**
     * 셋업 완료 (규칙/정기회의 확정 후)
     */
    public void completeSetup() {
        if (this.workflow != Workflow.SETUP) {
            throw new IllegalStateException("셋업 단계에서만 완료할 수 있습니다");
        }
        this.workflow = Workflow.COMPLETED;
    }
}
