package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.MeetingType;
import com.yamoyo.be.domain.teamroom.entity.TeamRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Table(name = "meeting_series")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_series_id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id", nullable = false)
    private TeamRoom teamRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false, length = 30)
    private MeetingType meetingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 3)
    private DayOfWeek dayOfWeek;

    @Column(name = "default_start_time", nullable = false)
    private LocalTime defaultStartTime;

    @Column(name = "default_duration_minutes", nullable = false)
    private Integer defaultDurationMinutes;

    @Column(name = "creator_name", nullable = false, length = 50)
    private String creatorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private MeetingSeries(TeamRoom teamRoom, MeetingType meetingType, DayOfWeek dayOfWeek,
                          LocalTime defaultStartTime, Integer defaultDurationMinutes, String creatorName) {
        this.teamRoom = teamRoom;
        this.meetingType = meetingType;
        this.dayOfWeek = dayOfWeek;
        this.defaultStartTime = defaultStartTime;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.creatorName = creatorName;
    }

    public static MeetingSeries create(TeamRoom teamRoom, MeetingType meetingType,
                                       DayOfWeek dayOfWeek, LocalTime defaultStartTime,
                                       Integer defaultDurationMinutes, String creatorName) {
        return MeetingSeries.builder()
                .teamRoom(teamRoom)
                .meetingType(meetingType)
                .dayOfWeek(dayOfWeek)
                .defaultStartTime(defaultStartTime)
                .defaultDurationMinutes(defaultDurationMinutes)
                .creatorName(creatorName)
                .build();
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
}
