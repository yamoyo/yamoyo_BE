package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "meetings")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_series_id", nullable = false)
    private MeetingSeries meetingSeries;

    @Column(nullable = false)
    private String title;

    @Column
    private String location;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingColor color;

    @Column
    private String description;

    @Column(name = "is_individually_modified", nullable = false)
    private Boolean isIndividuallyModified;

    @Column(name = "created_at", nullable = false, updatable = false)
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
    public Meeting(MeetingSeries meetingSeries, String title, String location,
                   LocalDateTime startTime, Integer durationMinutes, MeetingColor color,
                   String description, Boolean isIndividuallyModified) {
        this.meetingSeries = meetingSeries;
        this.title = title;
        this.location = location;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.color = color;
        this.description = description;
        this.isIndividuallyModified = isIndividuallyModified != null ? isIndividuallyModified : false;
    }
}
