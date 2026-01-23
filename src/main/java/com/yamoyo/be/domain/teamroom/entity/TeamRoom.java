package com.yamoyo.be.domain.teamroom.entity;

import com.yamoyo.be.domain.teamroom.enums.Lifecycle;
import com.yamoyo.be.domain.teamroom.enums.Workflow;
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

    @Column(name = "title")
    private String title;

    @Column(name = "description")
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

    @Builder
    public static TeamRoom create(String title, String description, LocalDateTime deadline, Long bannerImageId) {
        TeamRoom teamRoom = new TeamRoom();
        teamRoom.title = title;
        teamRoom.description = description;
        teamRoom.deadline = deadline;
        teamRoom.bannerImageId = bannerImageId;
        teamRoom.lifecycle = Lifecycle.ACTIVE;
        teamRoom.workflow = Workflow.PENDING;
        return teamRoom;
    }
}
