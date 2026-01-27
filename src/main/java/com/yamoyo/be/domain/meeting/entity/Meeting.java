package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.MeetingColor;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingParticipant> participants = new ArrayList<>();

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
    private boolean isIndividuallyModified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Meeting(MeetingSeries meetingSeries, String title, String location,
                   LocalDateTime startTime, Integer durationMinutes, MeetingColor color,
                   String description, boolean isIndividuallyModified) {
        this.meetingSeries = meetingSeries;
        this.title = title;
        this.location = location;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.color = color;
        this.description = description;
        this.isIndividuallyModified = isIndividuallyModified;
    }

    public static Meeting create(MeetingSeries meetingSeries, String title, String location,
                                 LocalDateTime startTime, Integer durationMinutes,
                                 MeetingColor color, String description) {
        validateStartTime(startTime);
        validateDuration(durationMinutes);
        validateColor(color);

        return Meeting.builder()
                .meetingSeries(meetingSeries)
                .title(title)
                .location(location)
                .startTime(startTime)
                .durationMinutes(durationMinutes)
                .color(color)
                .description(description)
                .isIndividuallyModified(false)
                .build();
    }

    private static void validateStartTime(LocalDateTime startTime) {
        int minute = startTime.getMinute();
        if (minute != 0 && minute != 30) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_START_TIME);
        }
    }

    private static void validateDuration(Integer durationMinutes) {
        if (durationMinutes % 30 != 0) {
            throw new YamoyoException(ErrorCode.MEETING_INVALID_DURATION);
        }
    }

    private static void validateColor(MeetingColor color) {
        if (color == MeetingColor.PURPLE) {
            throw new YamoyoException(ErrorCode.MEETING_PURPLE_COLOR_FORBIDDEN);
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String description, String location,
                       LocalDateTime startTime, Integer durationMinutes, MeetingColor color) {
        validateStartTime(startTime);
        validateDuration(durationMinutes);

        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.color = color;
    }

    public void markAsIndividuallyModified() {
        this.isIndividuallyModified = true;
    }

    public boolean isIndividuallyModified() {
        return this.isIndividuallyModified;
    }
}
