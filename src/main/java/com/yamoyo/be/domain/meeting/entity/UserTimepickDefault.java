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
import java.util.EnumMap;
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserTimepickDefault(User user, PreferredBlock preferredBlock,
                                Long availabilityMon, Long availabilityTue, Long availabilityWed,
                                Long availabilityThu, Long availabilityFri, Long availabilitySat,
                                Long availabilitySun) {
        this.user = user;
        this.preferredBlock = preferredBlock;
        this.availabilityMon = availabilityMon;
        this.availabilityTue = availabilityTue;
        this.availabilityWed = availabilityWed;
        this.availabilityThu = availabilityThu;
        this.availabilityFri = availabilityFri;
        this.availabilitySat = availabilitySat;
        this.availabilitySun = availabilitySun;
    }

    public static UserTimepickDefault createWithAvailability(User user, Map<DayOfWeek, Long> bitmaps) {
        return UserTimepickDefault.builder()
                .user(user)
                .availabilityMon(bitmaps.getOrDefault(DayOfWeek.MON, 0L))
                .availabilityTue(bitmaps.getOrDefault(DayOfWeek.TUE, 0L))
                .availabilityWed(bitmaps.getOrDefault(DayOfWeek.WED, 0L))
                .availabilityThu(bitmaps.getOrDefault(DayOfWeek.THU, 0L))
                .availabilityFri(bitmaps.getOrDefault(DayOfWeek.FRI, 0L))
                .availabilitySat(bitmaps.getOrDefault(DayOfWeek.SAT, 0L))
                .availabilitySun(bitmaps.getOrDefault(DayOfWeek.SUN, 0L))
                .build();
    }

    public static UserTimepickDefault createWithPreferredBlock(User user, PreferredBlock preferredBlock) {
        return UserTimepickDefault.builder()
                .user(user)
                .preferredBlock(preferredBlock)
                .availabilityMon(0L)
                .availabilityTue(0L)
                .availabilityWed(0L)
                .availabilityThu(0L)
                .availabilityFri(0L)
                .availabilitySat(0L)
                .availabilitySun(0L)
                .build();
    }

    public Map<DayOfWeek, Long> getAvailabilityBitmaps() {
        Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
        bitmaps.put(DayOfWeek.MON, availabilityMon);
        bitmaps.put(DayOfWeek.TUE, availabilityTue);
        bitmaps.put(DayOfWeek.WED, availabilityWed);
        bitmaps.put(DayOfWeek.THU, availabilityThu);
        bitmaps.put(DayOfWeek.FRI, availabilityFri);
        bitmaps.put(DayOfWeek.SAT, availabilitySat);
        bitmaps.put(DayOfWeek.SUN, availabilitySun);
        return bitmaps;
    }

    public void updateAvailability(Map<DayOfWeek, Long> bitmaps) {
        this.availabilityMon = bitmaps.getOrDefault(DayOfWeek.MON, 0L);
        this.availabilityTue = bitmaps.getOrDefault(DayOfWeek.TUE, 0L);
        this.availabilityWed = bitmaps.getOrDefault(DayOfWeek.WED, 0L);
        this.availabilityThu = bitmaps.getOrDefault(DayOfWeek.THU, 0L);
        this.availabilityFri = bitmaps.getOrDefault(DayOfWeek.FRI, 0L);
        this.availabilitySat = bitmaps.getOrDefault(DayOfWeek.SAT, 0L);
        this.availabilitySun = bitmaps.getOrDefault(DayOfWeek.SUN, 0L);
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
