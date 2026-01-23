package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickParticipantStatus;
import com.yamoyo.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @Column(nullable = false, length = 20)
    private TimepickParticipantStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_block", length = 20)
    private PreferredBlock preferredBlock;

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
