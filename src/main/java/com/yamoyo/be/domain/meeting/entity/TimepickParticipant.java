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

    @Column(name = "availability_mon", nullable = false)
    private Long availabilityMon;

    @Column(name = "availability_tue", nullable = false)
    private Long availabilityTue;

    @Column(name = "availability_wed", nullable = false)
    private Long availabilityWed;

    @Column(name = "availability_thu", nullable = false)
    private Long availabilityThu;

    @Column(name = "availability_fri", nullable = false)
    private Long availabilityFri;

    @Column(name = "availability_sat", nullable = false)
    private Long availabilitySat;

    @Column(name = "availability_sun", nullable = false)
    private Long availabilitySun;

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
                                Long availabilityMon, Long availabilityTue, Long availabilityWed,
                                Long availabilityThu, Long availabilityFri, Long availabilitySat,
                                Long availabilitySun) {
        this.timepick = timepick;
        this.user = user;
        this.preferredBlock = preferredBlock;
        this.availabilityStatus = availabilityStatus;
        this.preferredBlockStatus = preferredBlockStatus;
        this.availabilityMon = availabilityMon;
        this.availabilityTue = availabilityTue;
        this.availabilityWed = availabilityWed;
        this.availabilityThu = availabilityThu;
        this.availabilityFri = availabilityFri;
        this.availabilitySat = availabilitySat;
        this.availabilitySun = availabilitySun;
    }

    public static TimepickParticipant create(Timepick timepick, User user) {
        return TimepickParticipant.builder()
                .timepick(timepick)
                .user(user)
                .availabilityStatus(TimepickParticipantStatus.PENDING)
                .preferredBlockStatus(TimepickParticipantStatus.PENDING)
                .availabilityMon(0L)
                .availabilityTue(0L)
                .availabilityWed(0L)
                .availabilityThu(0L)
                .availabilityFri(0L)
                .availabilitySat(0L)
                .availabilitySun(0L)
                .build();
    }

    public void submitAvailability(Map<DayOfWeek, Long> bitmaps) {
        this.availabilityMon = bitmaps.getOrDefault(DayOfWeek.MON, 0L);
        this.availabilityTue = bitmaps.getOrDefault(DayOfWeek.TUE, 0L);
        this.availabilityWed = bitmaps.getOrDefault(DayOfWeek.WED, 0L);
        this.availabilityThu = bitmaps.getOrDefault(DayOfWeek.THU, 0L);
        this.availabilityFri = bitmaps.getOrDefault(DayOfWeek.FRI, 0L);
        this.availabilitySat = bitmaps.getOrDefault(DayOfWeek.SAT, 0L);
        this.availabilitySun = bitmaps.getOrDefault(DayOfWeek.SUN, 0L);
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
