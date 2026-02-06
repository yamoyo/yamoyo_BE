package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Table(name = "user_timepick_defaults")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTimepickDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_timepick_default_id", updatable = false, nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_block", length = 20)
    private PreferredBlock preferredBlock;

    @Embedded
    private WeeklyAvailability availability = WeeklyAvailability.empty();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserTimepickDefault(User user, PreferredBlock preferredBlock, WeeklyAvailability availability) {
        this.user = user;
        this.preferredBlock = preferredBlock;
        this.availability = availability;
    }

    public static UserTimepickDefault createWithAvailability(User user, Map<DayOfWeek, Long> bitmaps) {
        return UserTimepickDefault.builder()
                .user(user)
                .availability(WeeklyAvailability.from(bitmaps))
                .build();
    }

    public static UserTimepickDefault createWithPreferredBlock(User user, PreferredBlock preferredBlock) {
        return UserTimepickDefault.builder()
                .user(user)
                .preferredBlock(preferredBlock)
                .availability(WeeklyAvailability.empty())
                .build();
    }

    public Map<DayOfWeek, Long> getAvailabilityBitmaps() {
        return availability.toBitmaps();
    }

    public void updateAvailability(Map<DayOfWeek, Long> bitmaps) {
        this.availability.update(bitmaps);
    }

    public void updatePreferredBlock(PreferredBlock preferredBlock) {
        this.preferredBlock = preferredBlock;
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
