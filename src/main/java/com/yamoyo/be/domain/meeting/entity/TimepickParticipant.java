package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickParticipantStatus;
import com.yamoyo.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Table(name = "timepick_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"timepick_id", "user_id"}))
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimepickParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timepick_participant_id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timepick_id", nullable = false)
    private Timepick timepick;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_block", length = 20)
    private PreferredBlock preferredBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private TimepickParticipantStatus availabilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_block_status", nullable = false, length = 20)
    private TimepickParticipantStatus preferredBlockStatus;

    @Embedded
    private WeeklyAvailability availability = WeeklyAvailability.empty();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private TimepickParticipant(Timepick timepick, User user, PreferredBlock preferredBlock,
                                TimepickParticipantStatus availabilityStatus,
                                TimepickParticipantStatus preferredBlockStatus,
                                WeeklyAvailability availability) {
        this.timepick = timepick;
        this.user = user;
        this.preferredBlock = preferredBlock;
        this.availabilityStatus = availabilityStatus;
        this.preferredBlockStatus = preferredBlockStatus;
        this.availability = availability;
    }

    public static TimepickParticipant create(Timepick timepick, User user) {
        return TimepickParticipant.builder()
                .timepick(timepick)
                .user(user)
                .availabilityStatus(TimepickParticipantStatus.PENDING)
                .preferredBlockStatus(TimepickParticipantStatus.PENDING)
                .availability(WeeklyAvailability.empty())
                .build();
    }

    public void submitAvailability(Map<DayOfWeek, Long> bitmaps) {
        this.availability.update(bitmaps);
        this.availabilityStatus = TimepickParticipantStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    public void submitPreferredBlock(PreferredBlock preferredBlock) {
        this.preferredBlock = preferredBlock;
        this.preferredBlockStatus = TimepickParticipantStatus.SUBMITTED;
    }

    public boolean hasSubmittedAvailability() {
        return this.availabilityStatus == TimepickParticipantStatus.SUBMITTED;
    }

    public boolean hasSubmittedPreferredBlock() {
        return this.preferredBlockStatus == TimepickParticipantStatus.SUBMITTED;
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
